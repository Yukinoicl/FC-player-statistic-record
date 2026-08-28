export interface PlayerSummary {
  id: number
  name: string
}

export interface StatLine {
  appearances: number
  goals: number
  assists: number
  titles: number
  rating: number
}

export interface OverallStat {
  appearances: number
  goals: number
  assists: number
  leagueTitles: number
  continentalTitles: number
  rating: number
}

export interface SeasonRecord {
  id: number
  seasonName: string
  age: number
  continental: StatLine
  league: StatLine
}

export interface CareerTotals {
  latestAge: number
  continental: StatLine
  league: StatLine
  overall: OverallStat
}

export interface PlayerDetail {
  id: number
  name: string
  seasons: SeasonRecord[]
  totals: CareerTotals
}

export interface SeasonPayload {
  seasonName: string
  age: number
  continentalAppearances: number
  continentalGoals: number
  continentalAssists: number
  continentalTitles: number
  continentalRating: number
  leagueAppearances: number
  leagueGoals: number
  leagueAssists: number
  leagueTitles: number
  leagueRating: number
}
