<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { api } from './api'
import SeasonTable from './components/SeasonTable.vue'
import { useI18n } from './i18n'
import type { PlayerDetail, PlayerSummary, SeasonPayload, SeasonRecord } from './types'

const STORAGE_KEY = 'fc26-selected-player'
const { locale, t, setLocale, errorText } = useI18n()

const players = ref<PlayerSummary[]>([])
const selectedId = ref<number | null>(null)
const detail = ref<PlayerDetail | null>(null)
const loading = ref(false)
const toast = ref('')
const error = ref('')
const showAddPlayer = ref(false)
const newPlayerName = ref('')
const renaming = ref(false)
const draftName = ref('')
const autoEditId = ref<number | null>(null)
const dragId = ref<number | null>(null)
const overId = ref<number | null>(null)
let skipTabClick = false

let toastTimer: number | undefined

function showToast(message: string) {
  toast.value = message
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value = ''
  }, 2200)
}

function toCount(value: unknown): number {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) return 0
  return Math.round(n)
}

function toRating(value: unknown): number {
  const n = Number(value)
  if (!Number.isFinite(n) || n < 0) return 0
  return Math.min(10, Math.round(n * 100) / 100)
}

function toPayload(season: SeasonRecord): SeasonPayload {
  return {
    seasonName: String(season.seasonName || '').trim() || t('unnamedSeason'),
    age: toCount(season.age),
    continentalAppearances: toCount(season.continental.appearances),
    continentalGoals: toCount(season.continental.goals),
    continentalAssists: toCount(season.continental.assists),
    continentalTitles: toCount(season.continental.titles),
    continentalRating: toRating(season.continental.rating),
    leagueAppearances: toCount(season.league.appearances),
    leagueGoals: toCount(season.league.goals),
    leagueAssists: toCount(season.league.assists),
    leagueTitles: toCount(season.league.titles),
    leagueRating: toRating(season.league.rating),
  }
}

async function loadPlayers(preferId?: number | null) {
  players.value = await api.listPlayers()
  const stored = Number(localStorage.getItem(STORAGE_KEY))
  const nextId = preferId
    ?? (players.value.some((player) => player.id === selectedId.value) ? selectedId.value : null)
    ?? (players.value.some((player) => player.id === stored) ? stored : null)
    ?? players.value[0]?.id
    ?? null
  selectedId.value = nextId
  if (nextId) {
    localStorage.setItem(STORAGE_KEY, String(nextId))
  } else {
    localStorage.removeItem(STORAGE_KEY)
    detail.value = null
  }
}

async function loadDetail(id: number) {
  loading.value = true
  error.value = ''
  try {
    detail.value = await api.getPlayer(id)
    draftName.value = detail.value.name
  } catch (err) {
    detail.value = null
    error.value = errorText(err, 'loadFailed')
  } finally {
    loading.value = false
  }
}

async function refresh() {
  error.value = ''
  try {
    await loadPlayers()
  } catch (err) {
    error.value = errorText(err, 'loadFailed')
  }
}

watch(selectedId, (id) => {
  if (id == null) {
    detail.value = null
    return
  }
  localStorage.setItem(STORAGE_KEY, String(id))
  void loadDetail(id)
})

onMounted(() => {
  void refresh()
})

function openAddPlayer() {
  newPlayerName.value = ''
  showAddPlayer.value = true
}

async function createPlayer() {
  const name = newPlayerName.value.trim()
  if (!name) return
  try {
    const created = await api.createPlayer(name)
    showAddPlayer.value = false
    await loadPlayers(created.id)
    showToast(t('playerAdded'))
  } catch (err) {
    error.value = errorText(err, 'addFailed')
  }
}

function startRename() {
  if (!detail.value) return
  draftName.value = detail.value.name
  renaming.value = true
}

async function commitRename() {
  if (!detail.value) return
  const name = draftName.value.trim()
  renaming.value = false
  if (!name || name === detail.value.name) {
    draftName.value = detail.value.name
    return
  }
  try {
    const renamed = await api.renamePlayer(detail.value.id, name)
    detail.value.name = renamed.name
    const tab = players.value.find((player) => player.id === renamed.id)
    if (tab) tab.name = renamed.name
    showToast(t('nameUpdated'))
  } catch (err) {
    draftName.value = detail.value.name
    error.value = errorText(err, 'renameFailed')
  }
}

async function removePlayer() {
  if (!detail.value) return
  const ok = window.confirm(t('deletePlayerConfirm', { name: detail.value.name }))
  if (!ok) return
  const id = detail.value.id
  try {
    await api.deletePlayer(id)
    if (selectedId.value === id) selectedId.value = null
    await loadPlayers()
    showToast(t('playerDeleted'))
  } catch (err) {
    error.value = errorText(err, 'deleteFailed')
  }
}

async function addSeason() {
  if (!detail.value) return
  try {
    detail.value = await api.addSeason(detail.value.id)
    autoEditId.value = detail.value.seasons.at(-1)?.id ?? null
    showToast(t('seasonAdded'))
  } catch (err) {
    error.value = errorText(err, 'addSeasonFailed')
  }
}

function saveSeason(season: SeasonRecord, done: (ok: boolean) => void) {
  if (!detail.value) {
    done(false)
    return
  }
  void api.updateSeason(detail.value.id, season.id, toPayload(season))
    .then((result) => {
      detail.value = result
      showToast(t('saved'))
      done(true)
    })
    .catch((err: unknown) => {
      error.value = errorText(err, 'saveFailed')
      done(false)
    })
}

async function removeSeason(season: SeasonRecord) {
  if (!detail.value) return
  const ok = window.confirm(t('deleteSeasonConfirm', { age: season.age }))
  if (!ok) return
  try {
    detail.value = await api.deleteSeason(detail.value.id, season.id)
    showToast(t('seasonDeleted'))
  } catch (err) {
    error.value = errorText(err, 'deleteSeasonFailed')
  }
}

function onAutoEditConsumed() {
  autoEditId.value = null
}

function selectPlayer(id: number) {
  if (skipTabClick) {
    skipTabClick = false
    return
  }
  selectedId.value = id
}

function onTabDragStart(event: DragEvent, id: number) {
  dragId.value = id
  skipTabClick = true
  event.dataTransfer?.setData('text/plain', String(id))
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onTabDragOver(event: DragEvent, id: number) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  if (dragId.value !== id) {
    overId.value = id
  }
}

function movePlayer(fromId: number, toId: number) {
  if (fromId === toId) return false
  const list = [...players.value]
  const from = list.findIndex((player) => player.id === fromId)
  if (from < 0) return false
  const [item] = list.splice(from, 1)
  const to = toId < 0 ? list.length : list.findIndex((player) => player.id === toId)
  if (to < 0) {
    list.splice(from, 0, item)
    return false
  }
  list.splice(to, 0, item)
  players.value = list
  return true
}

function onTabDrop(event: DragEvent, targetId: number) {
  event.preventDefault()
  const fromId = dragId.value
  overId.value = null
  if (fromId == null) return
  if (movePlayer(fromId, targetId)) {
    void persistOrder()
  }
}

function onDropToEnd(event: DragEvent) {
  event.preventDefault()
  const fromId = dragId.value
  overId.value = null
  if (fromId == null) return
  if (movePlayer(fromId, -1)) {
    void persistOrder()
  }
}

function onTabDragEnd() {
  dragId.value = null
  overId.value = null
  window.setTimeout(() => {
    skipTabClick = false
  }, 80)
}

async function persistOrder() {
  try {
    players.value = await api.reorderPlayers(players.value.map((player) => player.id))
  } catch (err) {
    error.value = errorText(err, 'reorderFailed')
    try {
      await loadPlayers(selectedId.value)
    } catch {
      /* keep current list */
    }
  }
}

function formatRating(value: number): string {
  return value.toFixed(2)
}
</script>

<template>
  <div class="page">
    <header class="hero">
      <div>
        <p class="eyebrow">EA Sports FC 26</p>
        <h1>{{ t('appTitle') }}</h1>
      </div>
      <div class="lang-switch" role="group" :aria-label="t('langZh') + ' / ' + t('langEn')">
        <button type="button" :class="{ active: locale === 'zh' }" @click="setLocale('zh')">
          {{ t('langZh') }}
        </button>
        <button type="button" :class="{ active: locale === 'en' }" @click="setLocale('en')">
          {{ t('langEn') }}
        </button>
      </div>
    </header>

    <section class="panel">
      <div class="tabs">
        <button
          v-for="player in players"
          :key="player.id"
          class="tab"
          :class="{
            active: player.id === selectedId,
            dragging: player.id === dragId,
            'drag-over': player.id === overId,
          }"
          type="button"
          draggable="true"
          @click="selectPlayer(player.id)"
          @dragstart="onTabDragStart($event, player.id)"
          @dragover="onTabDragOver($event, player.id)"
          @drop="onTabDrop($event, player.id)"
          @dragend="onTabDragEnd"
        >
          {{ player.name }}
        </button>
        <button
          class="tab add"
          type="button"
          @click="openAddPlayer"
          @dragover.prevent
          @drop="onDropToEnd"
        >
          {{ t('addPlayerTab') }}
        </button>
      </div>

      <p v-if="error" class="banner error">{{ error }}</p>

      <div v-if="!players.length && !error" class="empty-card">
        <h2>{{ t('emptyTitle') }}</h2>
        <p>{{ t('emptyHint') }}</p>
        <button class="primary" type="button" @click="openAddPlayer">{{ t('addPlayer') }}</button>
      </div>

      <div v-else-if="loading && !detail" class="empty-card">
        <p>{{ t('loading') }}</p>
      </div>

      <div v-else-if="detail" class="player-board">
        <div class="player-head">
          <div>
            <input
              v-if="renaming"
              v-model="draftName"
              class="name-input"
              maxlength="80"
              @blur="commitRename"
              @keyup.enter="commitRename"
            />
            <button v-else class="player-name" type="button" @click="startRename">
              {{ detail.name }}
            </button>
          </div>
          <button class="ghost danger" type="button" @click="removePlayer">{{ t('deletePlayer') }}</button>
        </div>

        <div class="summary">
          <article>
            <span>{{ t('careerApps') }}</span>
            <ul class="split-stat">
              <li><em>{{ t('ucl') }}</em><b>{{ detail.totals.continental.appearances }}</b></li>
              <li><em>{{ t('league') }}</em><b>{{ detail.totals.league.appearances }}</b></li>
              <li class="all"><em>{{ t('all') }}</em><b>{{ detail.totals.overall.appearances }}</b></li>
            </ul>
          </article>
          <article>
            <span>{{ t('goals') }}</span>
            <ul class="split-stat">
              <li><em>{{ t('ucl') }}</em><b>{{ detail.totals.continental.goals }}</b></li>
              <li><em>{{ t('league') }}</em><b>{{ detail.totals.league.goals }}</b></li>
              <li class="all"><em>{{ t('all') }}</em><b>{{ detail.totals.overall.goals }}</b></li>
            </ul>
          </article>
          <article>
            <span>{{ t('assists') }}</span>
            <ul class="split-stat">
              <li><em>{{ t('ucl') }}</em><b>{{ detail.totals.continental.assists }}</b></li>
              <li><em>{{ t('league') }}</em><b>{{ detail.totals.league.assists }}</b></li>
              <li class="all"><em>{{ t('all') }}</em><b>{{ detail.totals.overall.assists }}</b></li>
            </ul>
          </article>
          <article>
            <span>{{ t('careerRating') }}</span>
            <strong class="green">
              {{ detail.totals.overall.appearances ? formatRating(detail.totals.overall.rating) : '—' }}
            </strong>
          </article>
          <article>
            <span>{{ t('titlesShort') }}</span>
            <strong>{{ detail.totals.overall.leagueTitles }} / {{ detail.totals.overall.continentalTitles }}</strong>
          </article>
        </div>

        <SeasonTable
          :seasons="detail.seasons"
          :auto-edit-id="autoEditId"
          @save="saveSeason"
          @remove="removeSeason"
          @auto-edit-consumed="onAutoEditConsumed"
        />

        <div class="table-actions">
          <button class="primary" type="button" @click="addSeason">{{ t('addSeason') }}</button>
        </div>
      </div>
    </section>

    <div v-if="showAddPlayer" class="overlay" @click.self="showAddPlayer = false">
      <form class="modal" @submit.prevent="createPlayer">
        <h2>{{ t('addPlayer') }}</h2>
        <label>
          {{ t('playerName') }}
          <input v-model="newPlayerName" maxlength="80" :placeholder="t('playerNamePlaceholder')" autofocus />
        </label>
        <div class="modal-actions">
          <button class="ghost" type="button" @click="showAddPlayer = false">{{ t('cancel') }}</button>
          <button class="primary" type="submit" :disabled="!newPlayerName.trim()">{{ t('confirm') }}</button>
        </div>
      </form>
    </div>

    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>

<style scoped>
.page {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 64px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.lang-switch {
  display: inline-flex;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 3px;
  background: var(--card);
}

.lang-switch button {
  border: 0;
  background: transparent;
  color: var(--muted);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
}

.lang-switch button.active {
  background: var(--green);
  color: var(--bg);
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--green);
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.panel {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 80%), var(--card);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow);
  overflow: hidden;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px;
  border-bottom: 1px solid var(--line);
  background: rgba(0, 0, 0, 0.16);
}

.tab {
  border: 1px solid transparent;
  background: transparent;
  color: var(--muted);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: grab;
}

.tab:active {
  cursor: grabbing;
}

.tab.active {
  color: var(--bg);
  background: var(--green);
  font-weight: 700;
}

.tab.add {
  border-color: var(--line);
  color: var(--text);
  cursor: pointer;
}

.tab.add:hover,
.tab:hover {
  border-color: rgba(62, 224, 127, 0.4);
}

.tab.dragging {
  opacity: 0.4;
}

.tab.drag-over {
  border-color: var(--green);
  box-shadow: inset 3px 0 0 var(--green);
}

.banner {
  margin: 16px 16px 0;
  padding: 10px 12px;
  border-radius: 12px;
}

.banner.error {
  background: var(--danger-dim);
  color: var(--danger);
}

.empty-card,
.player-board {
  padding: 20px 16px 24px;
}

.empty-card {
  text-align: center;
  padding: 64px 16px;
}

.empty-card h2 {
  margin: 0 0 8px;
}

.player-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
  margin-bottom: 18px;
}

.player-name,
.name-input {
  border: 0;
  background: transparent;
  color: var(--text);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.04em;
  padding: 0;
}

.name-input {
  border-bottom: 1px solid var(--green);
  outline: none;
  width: min(420px, 100%);
}

.summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.summary article {
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--card-2);
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  min-height: 118px;
}

.summary span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.summary strong {
  font-size: 24px;
  margin-top: auto;
}

.split-stat {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 4px;
}

.split-stat li {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}

.split-stat em {
  font-style: normal;
  color: var(--muted);
  font-size: 12px;
}

.split-stat b {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.split-stat .all b {
  color: var(--gold);
}

.green {
  color: var(--green);
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.primary,
.ghost {
  border-radius: 12px;
  padding: 10px 16px;
  border: 1px solid transparent;
}

.primary {
  background: var(--green);
  color: #062013;
  font-weight: 700;
}

.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ghost {
  background: transparent;
  color: var(--text);
  border-color: var(--line);
}

.ghost.danger {
  color: var(--danger);
  border-color: transparent;
  background: var(--danger-dim);
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(5, 8, 12, 0.64);
  display: grid;
  place-items: center;
  padding: 16px;
}

.modal {
  width: min(420px, 100%);
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 20px;
  box-shadow: var(--shadow);
}

.modal h2 {
  margin: 0 0 16px;
}

.modal label {
  display: grid;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
}

.modal input {
  border: 1px solid var(--line);
  background: var(--bg-2);
  color: var(--text);
  border-radius: 12px;
  padding: 10px 12px;
  outline: none;
}

.modal input:focus {
  border-color: var(--green);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

.toast {
  position: fixed;
  right: 20px;
  bottom: 20px;
  background: #10261a;
  color: var(--green);
  border: 1px solid rgba(62, 224, 127, 0.3);
  border-radius: 12px;
  padding: 10px 14px;
}

@media (max-width: 800px) {
  h1 {
    font-size: 20px;
  }

  .summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-name,
  .name-input {
    font-size: 22px;
  }
}
</style>
