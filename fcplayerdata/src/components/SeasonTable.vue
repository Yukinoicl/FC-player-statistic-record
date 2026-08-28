<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from '../i18n'
import type { SeasonRecord, StatLine } from '../types'

const props = defineProps<{
  seasons: SeasonRecord[]
  autoEditId?: number | null
}>()

const emit = defineEmits<{
  save: [season: SeasonRecord, done: (ok: boolean) => void]
  remove: [season: SeasonRecord]
  autoEditConsumed: []
}>()

const { t } = useI18n()

const editingId = ref<number | null>(null)
const draft = ref<SeasonRecord | null>(null)
const confirming = ref(false)
const ageInput = ref<HTMLInputElement | null>(null)

const displaySeasons = computed(() =>
  props.seasons.map((season) => {
    const current = rowOf(season)
    return {
      season,
      current,
      total: seasonTotal(current),
      editing: editingId.value === season.id,
    }
  }),
)

watch(
  () => props.autoEditId,
  (id) => {
    if (id == null) return
    const season = props.seasons.find((item) => item.id === id)
    if (season) startEdit(season)
    emit('autoEditConsumed')
  },
)

function bindAgeInput(el: unknown) {
  ageInput.value = el instanceof HTMLInputElement ? el : null
}

function cloneSeason(season: SeasonRecord): SeasonRecord {
  return {
    id: season.id,
    seasonName: season.seasonName,
    age: season.age,
    continental: { ...season.continental },
    league: { ...season.league },
  }
}

function startEdit(season: SeasonRecord) {
  editingId.value = season.id
  draft.value = cloneSeason(season)
  void nextTick(() => ageInput.value?.focus())
}

function cancelEdit() {
  if (confirming.value) return
  editingId.value = null
  draft.value = null
}

function confirmEdit() {
  if (!draft.value || confirming.value) return
  confirming.value = true
  emit('save', cloneSeason(draft.value), (ok) => {
    confirming.value = false
    if (ok) {
      editingId.value = null
      draft.value = null
    }
  })
}

function rowOf(season: SeasonRecord): SeasonRecord {
  return editingId.value === season.id && draft.value ? draft.value : season
}

function formatRating(value: number): string {
  return value.toFixed(2)
}

function ratingClass(value: number, appearances: number): string {
  if (appearances <= 0) return 'muted'
  if (value >= 8) return 'high'
  if (value >= 7) return 'mid'
  return ''
}

function displayRating(value: number, appearances: number): string {
  return appearances > 0 ? formatRating(value) : '—'
}

function seasonTotal(season: SeasonRecord) {
  const continental = season.continental
  const league = season.league
  const appearances = n(continental.appearances) + n(league.appearances)
  const weighted =
    n(continental.appearances) * n(continental.rating) + n(league.appearances) * n(league.rating)
  return {
    appearances,
    goals: n(continental.goals) + n(league.goals),
    assists: n(continental.assists) + n(league.assists),
    leagueTitles: n(league.titles),
    continentalTitles: n(continental.titles),
    rating: appearances > 0 ? Math.round((weighted / appearances) * 100) / 100 : 0,
  }
}

function n(value: unknown): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function lineValue(line: StatLine, key: keyof StatLine): number {
  return n(line[key])
}
</script>

<template>
  <div class="table-wrap">
    <table class="stats">
      <thead>
        <tr>
          <th>{{ t('colAge') }}</th>
          <th>{{ t('colType') }}</th>
          <th>{{ t('colApps') }}</th>
          <th>{{ t('colGoals') }}</th>
          <th>{{ t('colAssists') }}</th>
          <th>{{ t('colLeagueTitles') }}</th>
          <th>{{ t('colUclTitles') }}</th>
          <th>{{ t('colRating') }}</th>
          <th>{{ t('colGoalsAssists') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <template v-if="seasons.length">
          <template v-for="row in displaySeasons" :key="row.season.id">
            <tr class="pair-top" :class="{ editing: row.editing }">
              <td rowspan="3" class="age-cell">
                <input
                  v-if="row.editing && draft"
                  :ref="bindAgeInput"
                  v-model.number="draft.age"
                  class="cell-input"
                  type="number"
                  min="0"
                  max="60"
                />
                <span v-else>{{ row.current.age }}</span>
              </td>
              <td><span class="tag ucl">{{ t('ucl') }}</span></td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.continental.appearances"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.continental, 'appearances') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.continental.goals"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.continental, 'goals') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.continental.assists"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.continental, 'assists') }}</span>
              </td>
              <td class="muted-cell">—</td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.continental.titles"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.continental, 'titles') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.continental.rating"
                  class="cell-input rating"
                  type="number"
                  min="0"
                  max="10"
                  step="0.01"
                />
                <span
                  v-else
                  :class="ratingClass(row.season.continental.rating, row.season.continental.appearances)"
                >
                  {{ displayRating(row.season.continental.rating, row.season.continental.appearances) }}
                </span>
              </td>
              <td rowspan="3" class="ga-cell">
                <span class="ga">{{ row.total.goals }}/{{ row.total.assists }}</span>
              </td>
              <td rowspan="3" class="actions">
                <template v-if="row.editing">
                  <button class="link-ok" type="button" :disabled="confirming" @click="confirmEdit">
                    {{ confirming ? t('saving') : t('ok') }}
                  </button>
                  <button class="link-muted" type="button" :disabled="confirming" @click="cancelEdit">
                    {{ t('cancel') }}
                  </button>
                </template>
                <template v-else>
                  <button class="link-edit" type="button" @click="startEdit(row.season)">{{ t('edit') }}</button>
                  <button class="link-danger" type="button" @click="emit('remove', row.season)">{{ t('delete') }}</button>
                </template>
              </td>
            </tr>
            <tr class="pair-mid" :class="{ editing: row.editing }">
              <td><span class="tag league">{{ t('league') }}</span></td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.league.appearances"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.league, 'appearances') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.league.goals"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.league, 'goals') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.league.assists"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.league, 'assists') }}</span>
              </td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.league.titles"
                  class="cell-input"
                  type="number"
                  min="0"
                />
                <span v-else>{{ lineValue(row.season.league, 'titles') }}</span>
              </td>
              <td class="muted-cell">—</td>
              <td>
                <input
                  v-if="row.editing && draft"
                  v-model.number="draft.league.rating"
                  class="cell-input rating"
                  type="number"
                  min="0"
                  max="10"
                  step="0.01"
                />
                <span v-else :class="ratingClass(row.season.league.rating, row.season.league.appearances)">
                  {{ displayRating(row.season.league.rating, row.season.league.appearances) }}
                </span>
              </td>
            </tr>
            <tr class="pair-total" :class="{ editing: row.editing }">
              <td><span class="tag all">{{ t('total') }}</span></td>
              <td>{{ row.total.appearances }}</td>
              <td>{{ row.total.goals }}</td>
              <td>{{ row.total.assists }}</td>
              <td>{{ row.total.leagueTitles }}</td>
              <td>{{ row.total.continentalTitles }}</td>
              <td :class="ratingClass(row.total.rating, row.total.appearances)">
                {{ displayRating(row.total.rating, row.total.appearances) }}
              </td>
            </tr>
          </template>
        </template>
        <tr v-else>
          <td class="empty" colspan="10">{{ t('emptySeasons') }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrap {
  overflow-x: auto;
}

.stats {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}

.stats th {
  padding: 10px 8px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-align: center;
  border-bottom: 1px solid var(--line);
}

.stats td {
  padding: 7px 10px;
  text-align: center;
  border-bottom: 1px solid var(--line);
  font-variant-numeric: tabular-nums;
  font-size: 14px;
}

.age-cell {
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}

.pair-top td {
  background: rgba(121, 167, 255, 0.04);
}

.pair-mid td {
  background: rgba(94, 234, 212, 0.04);
}

.pair-total td {
  background: rgba(240, 199, 94, 0.08);
  font-weight: 700;
  border-bottom-color: rgba(148, 175, 206, 0.32);
}

.editing td {
  background: rgba(62, 224, 127, 0.06);
}

.pair-total.editing td {
  background: rgba(240, 199, 94, 0.12);
}

.stats td.ga-cell {
  width: 108px;
  background: rgba(240, 199, 94, 0.08);
}

.cell-input {
  width: 64px;
  border: 1px solid rgba(62, 224, 127, 0.45);
  background: rgba(8, 12, 18, 0.7);
  color: var(--text);
  border-radius: 8px;
  padding: 5px 4px;
  text-align: center;
  outline: none;
}

.cell-input.rating {
  width: 72px;
}

.cell-input:focus {
  border-color: var(--green);
}

.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.tag.ucl {
  color: var(--ucl);
  background: rgba(121, 167, 255, 0.12);
}

.tag.league {
  color: var(--league);
  background: rgba(94, 234, 212, 0.12);
}

.tag.all {
  color: var(--gold);
  background: rgba(240, 199, 94, 0.12);
}

.muted-cell,
.muted {
  color: var(--muted);
}

.high {
  color: var(--green);
  font-weight: 700;
}

.mid {
  color: var(--gold);
  font-weight: 700;
}

.ga {
  display: block;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.1;
  color: var(--gold);
}

.stats td.actions {
  width: 64px;
  padding: 6px 2px !important;
}

.actions button {
  display: block;
  width: 100%;
  border: 0;
  background: transparent;
  font-size: 12px;
  line-height: 1.4;
  margin: 0;
  padding: 3px 0;
}

.link-edit {
  color: var(--ucl);
}

.link-ok {
  color: var(--green);
  font-weight: 700;
}

.link-muted {
  color: var(--muted);
}

.link-danger {
  color: var(--danger);
}

.actions button:hover:not(:disabled) {
  text-decoration: underline;
}

.actions button:disabled {
  opacity: 0.6;
  cursor: default;
}

.empty {
  padding: 36px 12px !important;
  color: var(--muted);
  font-weight: 400;
}
</style>
