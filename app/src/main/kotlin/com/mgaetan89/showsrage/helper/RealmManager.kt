package com.mgaetan89.showsrage.helper

import android.content.Context
import com.mgaetan89.showsrage.model.Episode
import com.mgaetan89.showsrage.model.History
import com.mgaetan89.showsrage.model.LogEntry
import com.mgaetan89.showsrage.model.LogLevel
import com.mgaetan89.showsrage.model.OmDbEpisode
import com.mgaetan89.showsrage.model.Quality
import com.mgaetan89.showsrage.model.RealmShowStat
import com.mgaetan89.showsrage.model.RealmString
import com.mgaetan89.showsrage.model.RootDir
import com.mgaetan89.showsrage.model.Schedule
import com.mgaetan89.showsrage.model.Serie
import com.mgaetan89.showsrage.model.Show
import com.mgaetan89.showsrage.model.ShowStat
import com.mgaetan89.showsrage.model.ShowsStat
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmConfiguration
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.Sort

object RealmManager {
    private var realm: Realm? = null

    fun clearHistory() {
        this.getRealm()?.executeTransaction {
            it.delete(History::class.java)
        }
    }

    fun clearSchedule() {
        this.getRealm()?.executeTransaction {
            it.delete(Schedule::class.java)
        }
    }

    fun close() {
        if (this.realm != null && !this.realm!!.isClosed) {
            this.realm!!.close()
            this.realm = null
        }
    }

    fun deleteShow(indexerId: Int) {
        this.getRealm()?.executeTransaction {
            // Remove the episodes associated to that show
            it.where(Episode::class.java).equalTo("indexerId", indexerId).findAll().deleteAllFromRealm()

            // Remove the quality associated to that show
            it.where(Quality::class.java).equalTo("indexerId", indexerId).findFirst()?.deleteFromRealm()

            // Remove the stat associated to that show
            it.where(RealmShowStat::class.java).equalTo("indexerId", indexerId).findFirst()?.deleteFromRealm()

            // Remove the show from the Show table
            it.where(Show::class.java).equalTo("indexerId", indexerId).findFirst()?.deleteFromRealm()
        }
    }

    fun getEpisode(episodeId: String, listener: RealmChangeListener<Episode>?): Episode? {
        val realm = this.getRealm() ?: return null
        val query = realm.where(Episode::class.java)
                .equalTo("id", episodeId)

        if (listener == null) {
            return query.findFirst()
        }

        val episode = query.findFirstAsync()
        episode.addChangeListener(listener)

        return episode
    }

    fun getEpisodes(episodeId: String, listener: RealmChangeListener<RealmResults<OmDbEpisode>>): RealmResults<OmDbEpisode>? {
        val realm = this.getRealm() ?: return null
        val episodes = realm.where(OmDbEpisode::class.java)
                .equalTo("id", episodeId)
                .findAllAsync()
        episodes.addChangeListener(listener)

        return episodes
    }

    fun getEpisodes(indexerId: Int, season: Int, reversedOrder: Boolean, listener: RealmChangeListener<RealmResults<Episode>>): RealmResults<Episode>? {
        val realm = this.getRealm() ?: return null
        val episodes = realm.where(Episode::class.java)
                .equalTo("indexerId", indexerId)
                .equalTo("season", season)
                .findAllSortedAsync("number", if (reversedOrder) Sort.DESCENDING else Sort.ASCENDING)
        episodes.addChangeListener(listener)

        return episodes
    }

    fun getHistory(listener: RealmChangeListener<RealmResults<History>>): RealmResults<History>? {
        val realm = this.getRealm() ?: return null
        val history = realm.where(History::class.java).findAllSortedAsync("date", Sort.DESCENDING)
        history.addChangeListener(listener)

        return history
    }

    fun getLogs(logLevel: LogLevel, groups: Array<String>?, listener: RealmChangeListener<RealmResults<LogEntry>>): RealmResults<LogEntry>? {
        val realm = this.getRealm() ?: return null
        val logLevels = this.getLogLevels(logLevel)
        val query = realm.where(LogEntry::class.java)

        if (groups != null && groups.isNotEmpty()) {
            query.`in`("group", groups)
        }

        if (logLevels.isNotEmpty()) {
            query.`in`("errorType", logLevels)
        }

        val logs = query.findAllSortedAsync("dateTime", Sort.DESCENDING)
        logs.addChangeListener(listener)

        return logs
    }

    fun getLogsGroup(): List<String> {
        val realm = this.getRealm() ?: return emptyList()

        val groups = realm.where(LogEntry::class.java)
                .distinct("group")
                .map { it.group }
                .filterNotNull()
                .sorted()

        return groups
    }

    fun getRootDirs(): RealmResults<RootDir>? {
        val realm = this.getRealm() ?: return null

        return realm.where(RootDir::class.java).findAll()
    }

    fun getSchedule(section: String, listener: RealmChangeListener<RealmResults<Schedule>>): RealmResults<Schedule>? {
        val realm = this.getRealm() ?: return null
        val schedule = realm.where(Schedule::class.java)
                .equalTo("section", section)
                .findAllSortedAsync("airDate")
        schedule.addChangeListener(listener)

        return schedule
    }

    fun getScheduleSections(): List<String> {
        val realm = this.getRealm() ?: return emptyList()

        return realm.where(Schedule::class.java)
                .distinct("section")
                .map { it.section }
    }

    fun getSeries(imdbId: String, listener: RealmChangeListener<RealmResults<Serie>>): RealmResults<Serie>? {
        val realm = this.getRealm() ?: return null
        val series = realm.where(Serie::class.java).equalTo("imdbId", imdbId).findAllAsync()
        series.addChangeListener(listener)

        return series
    }

    fun getShow(indexerId: Int, listener: RealmChangeListener<Show>? = null): Show? {
        val realm = this.getRealm() ?: return null
        val query = realm.where(Show::class.java).equalTo("indexerId", indexerId)

        if (listener == null) {
            return query.findFirst()
        }

        val show = query.findFirstAsync()
        show.addChangeListener(listener)

        return show
    }

    fun getShows(anime: Boolean?, listener: RealmChangeListener<RealmResults<Show>>?): RealmResults<Show>? {
        val realm = this.getRealm() ?: return null
        val query = realm.where(Show::class.java)

        if (anime != null) {
            query.equalTo("anime", if (anime) 1 else 0)
        }

        if (listener == null) {
            return query.findAllSorted("showName")
        }

        val shows = query.findAllSortedAsync("showName")
        shows.addChangeListener(listener)

        return shows
    }

    fun getShowsStats(listener: RealmChangeListener<RealmResults<ShowsStat>>): RealmResults<ShowsStat>? {
        // There might be no data yet. So we request all data to be sure to always received a valid object.
        val realm = this.getRealm() ?: return null
        val stats = realm.where(ShowsStat::class.java).findAllAsync()
        stats.addChangeListener(listener)

        return stats
    }

    fun getShowStat(indexerId: Int): RealmShowStat? {
        val realm = this.getRealm() ?: return null

        return realm.where(RealmShowStat::class.java)
                .equalTo("indexerId", indexerId)
                .findFirst()
    }

    fun init(context: Context, testContext: Context?) {
        val configuration = RealmConfiguration.Builder(context).let {
            if (testContext != null) {
                val testFile = "test.realm"

                it.assetFile(testContext, testFile)
                it.name(testFile)
            }

            it.schemaVersion(4)
            it.migration(Migration())
            it.build()
        }

        Realm.setDefaultConfiguration(configuration)

        this.close()

        this.realm = Realm.getDefaultInstance()
    }

    fun saveEpisode(episode: Episode, indexerId: Int, season: Int, episodeNumber: Int) {
        this.getRealm()?.executeTransaction {
            this.prepareEpisodeForSaving(episode, indexerId, season, episodeNumber)

            it.copyToRealmOrUpdate(episode)
        }
    }

    fun saveEpisode(episode: OmDbEpisode) {
        this.getRealm()?.executeTransaction {
            this.prepareEpisodeForSaving(episode)

            it.copyToRealmOrUpdate(episode)
        }
    }

    fun saveEpisodes(episodes: List<Episode>, indexerId: Int, season: Int) {
        this.getRealm()?.executeTransaction {
            episodes.forEach {
                this.prepareEpisodeForSaving(it, indexerId, season, it.number)
            }

            it.copyToRealmOrUpdate(episodes)
        }
    }

    fun saveHistory(histories: List<History>) {
        this.clearHistory()

        this.getRealm()?.executeTransaction {
            histories.forEach {
                this.prepareHistoryForSaving(it)
            }

            it.copyToRealmOrUpdate(histories)
        }
    }

    fun saveLogs(logs: List<LogEntry>, logLevel: LogLevel) {
        this.getRealm()?.executeTransaction {
            // Remove all existing logs for the level we are about to save
            it.where(LogEntry::class.java)
                    .equalTo("errorType", logLevel.name)
                    .findAll()
                    .deleteAllFromRealm()

            // Save the new logs in the database
            it.copyToRealm(logs)
        }
    }

    fun saveRootDirs(rootDirs: List<RootDir>) {
        this.clearRootDirs()

        this.getRealm()?.executeTransaction {
            it.copyToRealmOrUpdate(rootDirs)
        }
    }

    fun saveSchedules(section: String, schedules: List<Schedule>) {
        this.getRealm()?.executeTransaction {
            schedules.forEach {
                this.prepareScheduleForSaving(it, section)
            }

            it.copyToRealmOrUpdate(schedules)
        }
    }

    fun saveSerie(serie: Serie) {
        this.getRealm()?.executeTransaction {
            it.copyToRealmOrUpdate(serie)
        }
    }

    fun saveShow(show: Show) {
        this.getRealm()?.executeTransaction {
            this.prepareShowForSaving(show)

            it.copyToRealmOrUpdate(show)
        }
    }

    fun saveShows(shows: List<Show>) {
        this.getRealm()?.executeTransaction {
            shows.forEach {
                this.prepareShowForSaving(it)
            }

            it.copyToRealmOrUpdate(shows)
        }

        // Remove information about shows that might have been removed
        val savedShows = this.getShows(null, null) ?: return
        val removedIndexerIds = savedShows.map { it.indexerId } - shows.map { it.indexerId }

        removedIndexerIds.forEach {
            // deleteShow has its own transaction, so we need to run this outside of the above transaction
            this.deleteShow(it)
        }
    }

    fun saveShowsStat(stat: ShowsStat) {
        this.getRealm()?.executeTransaction {
            it.delete(ShowsStat::class.java)
            it.copyToRealm(stat)
        }
    }

    fun saveShowStat(stat: ShowStat, indexerId: Int): RealmShowStat {
        val realmStat = this.getRealmStat(stat, indexerId)

        this.getRealm()?.executeTransaction {
            it.copyToRealmOrUpdate(realmStat)
        }

        return realmStat
    }

    private fun clearRootDirs() {
        this.getRealm()?.executeTransaction {
            it.delete(RootDir::class.java)
        }
    }

    private fun getLogLevels(logLevel: LogLevel): Array<String> {
        return when (logLevel) {
            LogLevel.DEBUG -> arrayOf("DEBUG", "ERROR", "INFO", "WARNING")
            LogLevel.ERROR -> arrayOf("ERROR")
            LogLevel.INFO -> arrayOf("ERROR", "INFO", "WARNING")
            LogLevel.WARNING -> arrayOf("ERROR", "WARNING")
        }
    }

    internal fun getRealm(): Realm? {
        return if (this.realm?.isClosed ?: true) {
            null
        } else {
            this.realm
        }
    }

    private fun getRealmStat(stat: ShowStat, indexerId: Int): RealmShowStat {
        return RealmShowStat().apply {
            this.downloaded = stat.getTotalDone()
            this.episodesCount = stat.total
            this.indexerId = indexerId
            this.snatched = stat.getTotalPending()
        }
    }

    private fun prepareEpisodeForSaving(episode: Episode, indexerId: Int, season: Int, episodeNumber: Int) {
        val id = Episode.buildId(indexerId, season, episodeNumber)
        val savedEpisode = this.getEpisode(id, null)

        episode.description = if (episode.description.isNullOrEmpty()) savedEpisode?.description else episode.description
        episode.fileSizeHuman = if (episode.fileSizeHuman.isNullOrEmpty()) savedEpisode?.fileSizeHuman else episode.fileSizeHuman
        episode.id = id
        episode.indexerId = indexerId
        episode.number = episodeNumber
        episode.season = season
    }

    private fun prepareEpisodeForSaving(episode: OmDbEpisode) {
        episode.id = OmDbEpisode.buildId(episode.seriesId ?: "", episode.season ?: "", episode.episode ?: "")
    }

    private fun prepareHistoryForSaving(history: History) {
        history.id = "${history.date}_${history.status}_${history.indexerId}_${history.season}_${history.episode}"
    }

    private fun prepareScheduleForSaving(schedule: Schedule, section: String) {
        schedule.id = "${schedule.indexerId}_${schedule.season}_${schedule.episode}"
        schedule.section = section
    }

    private fun prepareShowForSaving(show: Show) {
        val savedShow = this.getShow(show.indexerId)

        show.airs = if (show.airs.isNullOrEmpty()) savedShow?.airs else show.airs
        show.genre = RealmList<RealmString>().apply {
            this.addAll((if (show.genre?.isEmpty() ?: true) savedShow?.genre else show.genre) ?: emptyList())
        }
        show.imdbId = if (show.imdbId.isNullOrEmpty()) savedShow?.imdbId else show.imdbId
        show.location = if (show.location.isNullOrEmpty()) savedShow?.location else show.location
        show.qualityDetails = Quality().apply {
            this.archive = RealmList<RealmString>().apply {
                this.addAll((if (show.qualityDetails?.archive?.isEmpty() ?: true) savedShow?.qualityDetails?.archive else show.qualityDetails?.archive) ?: emptyList())
            }
            this.indexerId = show.indexerId
            this.initial = RealmList<RealmString>().apply {
                this.addAll((if (show.qualityDetails?.initial?.isEmpty() ?: true) savedShow?.qualityDetails?.initial else show.qualityDetails?.initial) ?: emptyList())
            }
        }
        show.seasonList = RealmList<RealmString>().apply {
            this.addAll((if (show.seasonList?.isEmpty() ?: true) savedShow?.seasonList else show.seasonList) ?: emptyList())
        }
    }
}
