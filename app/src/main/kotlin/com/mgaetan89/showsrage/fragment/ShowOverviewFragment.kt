package com.mgaetan89.showsrage.fragment

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.customtabs.CustomTabsSession
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.NavUtils
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.graphics.Palette
import android.support.v7.widget.CardView
import android.text.format.DateUtils
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.mgaetan89.showsrage.Constants
import com.mgaetan89.showsrage.R
import com.mgaetan89.showsrage.activity.MainActivity
import com.mgaetan89.showsrage.helper.DateTimeHelper
import com.mgaetan89.showsrage.helper.GenericCallback
import com.mgaetan89.showsrage.helper.ImageLoader
import com.mgaetan89.showsrage.helper.Utils
import com.mgaetan89.showsrage.model.*
import com.mgaetan89.showsrage.network.OmDbApi
import com.mgaetan89.showsrage.network.SickRageApi
import retrofit.Callback
import retrofit.RestAdapter
import retrofit.RetrofitError
import retrofit.client.Response
import java.lang.ref.WeakReference

class ShowOverviewFragment : Fragment(), Callback<SingleShow>, View.OnClickListener, ImageLoader.OnImageResult, Palette.PaletteAsyncListener {
    private var airs: TextView? = null
    private var awards: TextView? = null
    private var awardsLayout: CardView? = null
    private var banner: ImageView? = null
    private var castingActors: TextView? = null
    private var castingDirectors: TextView? = null
    private var castingLayout: CardView? = null
    private var castingWriters: TextView? = null
    private var fanArt: ImageView? = null
    private var genre: TextView? = null
    private var imdb: Button? = null
    private var languageCountry: TextView? = null
    private var location: TextView? = null
    private var name: TextView? = null
    private var network: TextView? = null
    private var nextEpisodeDate: TextView? = null
    private var omDbApi: OmDbApi? = null
    private var pauseMenu: MenuItem? = null
    private var plot: TextView? = null
    private var plotLayout: CardView? = null
    private var poster: ImageView? = null
    private var quality: TextView? = null
    private var rated: TextView? = null
    private var rating: TextView? = null
    private var ratingStars: RatingBar? = null
    private var resumeMenu: MenuItem? = null
    private var runtime: TextView? = null
    private var serviceConnection: ServiceConnection? = null
    private var show: Show? = null
    private var status: TextView? = null
    private var tabSession: CustomTabsSession? = null
    private var theTvDb: Button? = null
    private var webSearch: Button? = null
    private var year: TextView? = null

    init {
        this.setHasOptionsMenu(true)
    }

    override fun failure(error: RetrofitError?) {
        error?.printStackTrace()
    }

    fun getSetShowQualityCallback(): Callback<GenericResponse> {
        return GenericCallback(this.activity);
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val restAdapter = RestAdapter.Builder()
                .setEndpoint(Constants.OMDB_URL)
                .setLogLevel(Constants.NETWORK_LOG_LEVEL)
                .build()

        this.omDbApi = restAdapter.create(OmDbApi::class.java)
        this.serviceConnection = ServiceConnection(this)
        this.show = this.arguments.getParcelable(Constants.Bundle.SHOW_MODEL)

        val activity = this.activity

        if (this.show != null) {
            activity?.title = this.show!!.showName

            SickRageApi.instance.services?.getShow(this.show!!.indexerId, this)
        }

        CustomTabsClient.bindCustomTabsService(this.context, "com.android.chrome", this.serviceConnection)
    }

    override fun onClick(view: View?) {
        if (view == null || this.show == null) {
            return
        }

        val activity = this.activity
        var color = ContextCompat.getColor(activity, R.color.primary)
        val url = when (view.id) {
            R.id.show_imdb -> "http://www.imdb.com/title/${this.show!!.imdbId}"
            R.id.show_the_tvdb -> "http://thetvdb.com/?tab=series&id=${this.show!!.tvDbId}"
            R.id.show_web_search -> {
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY, this.show!!.showName)

                this.startActivity(intent)

                return
            }
            else -> return
        }

        if (activity is MainActivity) {
            val colors = activity.getThemColors()

            if (colors != null) {
                color = colors.primary
            }
        }

        val tabIntent = CustomTabsIntent.Builder(this.tabSession)
                .addDefaultShareMenuItem()
                .enableUrlBarHiding()
                .setCloseButtonIcon(BitmapFactory.decodeResource(this.resources, R.drawable.ic_arrow_back_white_24dp))
                .setShowTitle(true)
                .setToolbarColor(color)
                .build()
        tabIntent.launchUrl(this.activity, Uri.parse(url))
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.show_overview, menu)

        this.pauseMenu = menu?.findItem(R.id.menu_pause_show)
        this.pauseMenu?.isVisible = false
        this.resumeMenu = menu?.findItem(R.id.menu_resume_show)
        this.resumeMenu?.isVisible = false

        if (this.show != null) {
            this.showHidePauseResumeMenus(this.show!!.paused == 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_show_overview, container, false)

        if (view != null) {
            this.airs = view.findViewById(R.id.show_airs) as TextView?
            this.awards = view.findViewById(R.id.show_awards) as TextView?
            this.awardsLayout = view.findViewById(R.id.show_awards_layout) as CardView?
            this.banner = view.findViewById(R.id.show_banner) as ImageView?
            this.castingActors = view.findViewById(R.id.show_casting_actors) as TextView?
            this.castingDirectors = view.findViewById(R.id.show_casting_directors) as TextView?
            this.castingLayout = view.findViewById(R.id.show_casting_layout) as CardView?
            this.castingWriters = view.findViewById(R.id.show_casting_writers) as TextView?
            this.fanArt = view.findViewById(R.id.show_fan_art) as ImageView?
            this.genre = view.findViewById(R.id.show_genre) as TextView?
            this.imdb = view.findViewById(R.id.show_imdb) as Button?
            this.languageCountry = view.findViewById(R.id.show_language_country) as TextView?
            this.location = view.findViewById(R.id.show_location) as TextView?
            this.name = view.findViewById(R.id.show_name) as TextView?
            this.network = view.findViewById(R.id.show_network) as TextView?
            this.nextEpisodeDate = view.findViewById(R.id.show_next_episode_date) as TextView?
            this.plot = view.findViewById(R.id.show_plot) as TextView?
            this.plotLayout = view.findViewById(R.id.show_plot_layout) as CardView?
            this.poster = view.findViewById(R.id.show_poster) as ImageView?
            this.quality = view.findViewById(R.id.show_quality) as TextView?
            this.rated = view.findViewById(R.id.show_rated) as TextView?
            this.rating = view.findViewById(R.id.show_rating) as TextView?
            this.ratingStars = view.findViewById(R.id.show_rating_stars) as RatingBar?
            this.runtime = view.findViewById(R.id.show_runtime) as TextView?
            this.status = view.findViewById(R.id.show_status) as TextView?
            this.theTvDb = view.findViewById(R.id.show_the_tvdb) as Button?
            this.webSearch = view.findViewById(R.id.show_web_search) as Button?
            this.year = view.findViewById(R.id.show_year) as TextView?

            this.imdb?.setOnClickListener(this)
            this.theTvDb?.setOnClickListener(this)
            this.webSearch?.setOnClickListener(this)
        }

        return view
    }

    override fun onDestroy() {
        val activity = this.activity

        if (activity is MainActivity) {
            activity.resetThemeColors()
        }

        if (this.serviceConnection != null) {
            this.context.unbindService(this.serviceConnection)
        }

        super.onDestroy()
    }

    override fun onDestroyView() {
        this.airs = null
        this.awards = null
        this.awardsLayout = null
        this.banner = null
        this.castingActors = null
        this.castingDirectors = null
        this.castingLayout = null
        this.castingWriters = null
        this.fanArt = null
        this.genre = null
        this.imdb = null
        this.languageCountry = null
        this.location = null
        this.name = null
        this.nextEpisodeDate = null
        this.network = null
        this.plot = null
        this.plotLayout = null
        this.poster = null
        this.quality = null
        this.rated = null
        this.rating = null
        this.ratingStars = null
        this.runtime = null
        this.status = null
        this.theTvDb = null
        this.webSearch = null
        this.year = null

        super.onDestroyView()
    }

    override fun onGenerated(palette: Palette?) {
        val activity = this.activity
        val colors = Utils.getThemeColors(this.context, palette!!)
        val colorPrimary = colors.primary

        if (activity is MainActivity) {
            activity.setThemeColors(colors)
        }

        if (colorPrimary == 0) {
            return
        }

        val colorStateList = ColorStateList.valueOf(colorPrimary)
        val textColor = Utils.getContrastColor(colorPrimary)

        if (this.imdb != null) {
            ViewCompat.setBackgroundTintList(this.imdb, colorStateList)
            this.imdb!!.setTextColor(textColor)
        }

        if (this.theTvDb != null) {
            ViewCompat.setBackgroundTintList(this.theTvDb, colorStateList)
            this.theTvDb!!.setTextColor(textColor)
        }

        if (this.webSearch != null) {
            ViewCompat.setBackgroundTintList(this.webSearch, colorStateList)
            this.webSearch!!.setTextColor(textColor)
        }
    }

    override fun onImageError(imageView: ImageView, exception: Exception?, errorDrawable: Drawable?) {
        val parent = imageView.parent

        if (parent is View) {
            parent.visibility = View.GONE
        }
    }

    override fun onImageReady(imageView: ImageView, resource: Bitmap?) {
        val parent = imageView.parent

        if (parent is View) {
            parent.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_change_quality -> {
                this.changeQuality()

                true
            }

            R.id.menu_delete_show -> {
                this.deleteShow()

                true
            }

            R.id.menu_pause_show -> {
                this.pauseOrResumeShow(true)

                true
            }

            R.id.menu_rescan_show -> {
                this.rescanShow()

                true
            }

            R.id.menu_resume_show -> {
                this.pauseOrResumeShow(false)

                true
            }

            R.id.menu_update_show -> {
                this.updateShow()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun success(singleShow: SingleShow?, response: Response?) {
        this.show = singleShow?.data ?: return
        val nextEpisodeAirDate = this.show!!.nextEpisodeAirDate

        this.showHidePauseResumeMenus(this.show!!.paused == 0)

        this.omDbApi!!.getShow(this.show!!.imdbId, OmdbShowCallback(this))

        if (this.airs != null) {
            val airs = this.show!!.airs

            this.airs!!.text = if (airs.isNullOrEmpty()) {
                this.getString(R.string.airs, "N/A")
            } else {
                this.getString(R.string.airs, airs)
            }

            this.airs!!.visibility = View.VISIBLE
        }

        if (this.banner != null) {
            ImageLoader.load(
                    this.banner,
                    SickRageApi.instance.getBannerUrl(this.show!!.tvDbId, Indexer.TVDB),
                    false, null, this
            )

            this.banner!!.contentDescription = this.show!!.showName
        }

        if (this.fanArt != null) {
            ImageLoader.load(
                    this.fanArt,
                    SickRageApi.instance.getFanArtUrl(this.show!!.tvDbId, Indexer.TVDB),
                    false, null, this
            )

            this.fanArt!!.contentDescription = this.show!!.showName
        }

        if (this.genre != null) {
            val genresList = this.show!!.genre

            if (genresList?.isNotEmpty() ?: false) {
                val genres = genresList.joinToString(", ")

                this.genre!!.text = this.getString(R.string.genre, genres)
                this.genre!!.visibility = View.VISIBLE
            } else {
                this.genre!!.visibility = View.GONE
            }
        }

        if (this.imdb != null) {
            this.imdb!!.visibility = if (this.show!!.imdbId.isNullOrEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        if (this.languageCountry != null) {
            this.languageCountry!!.text = this.getString(R.string.language_value, this.show!!.language)
            this.languageCountry!!.visibility = View.VISIBLE
        }

        if (this.location != null) {
            val location = this.show!!.location

            this.location!!.text = if (location.isNullOrEmpty()) {
                this.getString(R.string.location, "N/A")
            } else {
                this.getString(R.string.location, location)
            }
            this.location!!.visibility = View.VISIBLE
        }

        if (this.name != null) {
            this.name!!.text = this.show!!.showName
            this.name!!.visibility = View.VISIBLE
        }

        if (this.nextEpisodeDate != null) {
            if (nextEpisodeAirDate.isNullOrEmpty()) {
                this.nextEpisodeDate!!.visibility = View.GONE
            } else {
                this.nextEpisodeDate!!.text = this.getString(R.string.next_episode, DateTimeHelper.getRelativeDate(nextEpisodeAirDate, "yyyy-MM-dd", DateUtils.DAY_IN_MILLIS))
                this.nextEpisodeDate!!.visibility = View.VISIBLE
            }
        }

        if (this.network != null) {
            this.network!!.text = this.getString(R.string.network, this.show!!.network)
            this.network!!.visibility = View.VISIBLE
        }

        if (this.poster != null) {
            ImageLoader.load(
                    this.poster,
                    SickRageApi.instance.getPosterUrl(this.show!!.tvDbId, Indexer.TVDB),
                    false, this, null
            )

            this.poster!!.contentDescription = this.show!!.showName
        }

        if (this.quality != null) {
            val quality = this.show!!.quality

            if ("custom".equals(quality, true)) {
                val qualityDetails = this.show!!.qualityDetails
                val allowed = listToString(this.getTranslatedQualities(qualityDetails.initial, true))
                val preferred = listToString(this.getTranslatedQualities(qualityDetails.archive, false))

                this.quality!!.text = this.getString(R.string.quality_custom, allowed, preferred)
            } else {
                this.quality!!.text = this.getString(R.string.quality, quality)
            }

            this.quality!!.visibility = View.VISIBLE
        }

        if (this.status != null) {
            if (nextEpisodeAirDate.isNullOrEmpty()) {
                val status = this.show!!.statusTranslationResource

                this.status!!.text = if (status != 0) {
                    this.getString(status)
                } else {
                    this.show!!.status
                }
                this.status!!.visibility = View.VISIBLE
            } else {
                this.status!!.visibility = View.GONE
            }
        }
    }

    private fun changeQuality() {
        if (this.show == null) {
            return
        }

        val arguments = Bundle()
        arguments.putInt(Constants.Bundle.INDEXER_ID, this.show!!.indexerId)

        val fragment = ChangeQualityFragment()
        fragment.arguments = arguments
        fragment.show(this.childFragmentManager, "change_quality")
    }

    private fun deleteShow() {
        if (this.show == null) {
            return
        }

        val indexerId = this.show!!.indexerId
        val callback = DeleteShowCallback(this.activity)

        AlertDialog.Builder(this.context)
                .setTitle(this.getString(R.string.delete_show_title, this.show!!.showName))
                .setMessage(R.string.delete_show_message)
                .setPositiveButton(R.string.keep, { dialog, which ->
                    SickRageApi.instance.services?.deleteShow(indexerId, 0, callback)
                })
                .setNegativeButton(R.string.delete, { dialog, which ->
                    SickRageApi.instance.services?.deleteShow(indexerId, 1, callback)
                })
                .setNeutralButton(R.string.cancel, null)
                .show()
    }

    private fun getTranslatedQualities(qualities: Collection<String>?, allowed: Boolean): List<String> {
        val translatedQualities = mutableListOf<String>()

        if (qualities == null || qualities.isEmpty()) {
            return translatedQualities
        }

        val keys = if (allowed) {
            resources.getStringArray(R.array.allowed_qualities_keys).toList()
        } else {
            resources.getStringArray(R.array.allowed_qualities_keys).toList()
        }
        val values = if (allowed) {
            resources.getStringArray(R.array.allowed_qualities_values).toList()
        } else {
            resources.getStringArray(R.array.allowed_qualities_values).toList()
        }

        qualities.forEach {
            val position = keys.indexOf(it)

            if (position != -1) {
                // Skip the "Ignore" first item
                translatedQualities.add(values[position + 1])
            }
        }

        return translatedQualities
    }

    private fun pauseOrResumeShow(pause: Boolean) {
        if (this.show == null) {
            return
        }

        this.showHidePauseResumeMenus(!pause)

        SickRageApi.instance.services?.pauseShow(this.show!!.indexerId, if (pause) 1 else 0, object : GenericCallback(this.activity) {
            override fun failure(error: RetrofitError?) {
                super.failure(error)

                showHidePauseResumeMenus(pause)
            }
        })
    }

    private fun rescanShow() {
        if (this.show != null) {
            SickRageApi.instance.services?.rescanShow(this.show!!.indexerId, GenericCallback(this.activity))
        }
    }

    private fun showHidePauseResumeMenus(isPause: Boolean) {
        this.pauseMenu?.isVisible = isPause
        this.resumeMenu?.isVisible = !isPause
    }

    private fun updateShow() {
        if (this.show != null) {
            SickRageApi.instance.services?.updateShow(this.show!!.indexerId, GenericCallback(this.activity))
        }
    }

    private class DeleteShowCallback(activity: FragmentActivity) : GenericCallback(activity) {
        override fun success(genericResponse: GenericResponse?, response: Response?) {
            super.success(genericResponse, response)

            NavUtils.navigateUpFromSameTask(this.getActivity())
        }
    }

    private class OmdbShowCallback(fragment: ShowOverviewFragment) : Callback<Serie> {
        private val fragmentReference: WeakReference<ShowOverviewFragment>

        init {
            this.fragmentReference = WeakReference(fragment)
        }

        override fun failure(error: RetrofitError?) {
            error?.printStackTrace()
        }

        override fun success(serie: Serie?, response: Response?) {
            val fragment = this.fragmentReference.get() ?: return

            if (fragment.awards != null) {
                setText(fragment, fragment.awards!!, serie?.awards, 0, fragment.awardsLayout)
            }

            val actors = serie?.actors
            val director = serie?.director
            val writer = serie?.writer

            if (hasText(actors) || hasText(director) || hasText(writer)) {
                if (fragment.castingActors != null) {
                    setText(fragment, fragment.castingActors!!, actors, R.string.actors, null)
                }

                if (fragment.castingDirectors != null) {
                    setText(fragment, fragment.castingDirectors!!, director, R.string.directors, null)
                }

                fragment.castingLayout?.visibility = View.VISIBLE

                if (fragment.castingWriters != null) {
                    setText(fragment, fragment.castingWriters!!, writer, R.string.writers, null)
                }
            } else {
                fragment.castingLayout?.visibility = View.GONE
            }

            if (fragment.languageCountry != null) {
                val country = serie?.country
                val language = serie?.language

                if (hasText(language)) {
                    fragment.languageCountry!!.text = if (hasText(country)) {
                        fragment.getString(R.string.language_county, language, country)
                    } else {
                        fragment.getString(R.string.language_value, language)
                    }
                    fragment.languageCountry!!.visibility = View.VISIBLE
                } else {
                    fragment.languageCountry!!.visibility = View.GONE
                }
            }

            if (fragment.plot != null) {
                setText(fragment, fragment.plot!!, serie?.plot, 0, fragment.plotLayout)
            }

            if (fragment.rated != null) {
                setText(fragment, fragment.rated!!, serie?.rated, R.string.rated, null)
            }

            if (fragment.rating != null) {
                val imdbRating = serie?.imdbRating
                val imdbVotes = serie?.imdbVotes

                if (hasText(imdbRating) && hasText(imdbVotes)) {
                    fragment.rating!!.text = fragment.getString(R.string.rating, imdbRating, imdbVotes)
                    fragment.rating!!.visibility = View.VISIBLE
                } else {
                    fragment.rating!!.visibility = View.GONE
                }
            }

            if (fragment.ratingStars != null) {
                try {
                    fragment.ratingStars!!.rating = serie?.imdbRating?.toFloat() ?: 0f
                    fragment.ratingStars!!.visibility = View.VISIBLE
                } catch(exception: Exception) {
                    fragment.ratingStars!!.visibility = View.GONE
                }
            }

            if (fragment.runtime != null) {
                setText(fragment, fragment.runtime!!, serie?.runtime, R.string.runtime, null)
            }

            if (fragment.year != null) {
                setText(fragment, fragment.year!!, serie?.year, R.string.year, null)
            }
        }

        companion object {
            private fun hasText(text: String?): Boolean {
                return !text.isNullOrEmpty() && !"N/A".equals(text, true)
            }

            private fun setText(fragment: Fragment, textView: TextView, text: String?, label: Int, layout: View?) {
                if (hasText(text)) {
                    if (layout == null) {
                        textView.text = fragment.getString(label, text)
                        textView.visibility = View.VISIBLE
                    } else {
                        layout.visibility = View.VISIBLE
                        textView.text = text
                    }
                } else {
                    layout?.visibility = View.GONE
                    textView.visibility = View.GONE
                }
            }
        }
    }

    private class ServiceConnection(fragment: ShowOverviewFragment) : CustomTabsServiceConnection() {
        private val fragmentReference: WeakReference<ShowOverviewFragment>

        init {
            this.fragmentReference = WeakReference(fragment)
        }

        override fun onCustomTabsServiceConnected(componentName: ComponentName?, customTabsClient: CustomTabsClient?) {
            customTabsClient?.warmup(0L)

            val fragment = this.fragmentReference.get() ?: return
            fragment.tabSession = customTabsClient?.newSession(null)
            fragment.tabSession?.mayLaunchUrl(Uri.parse("http://www.imdb.com/title/${fragment.show!!.imdbId}"), null, null)
            fragment.tabSession?.mayLaunchUrl(Uri.parse("http://thetvdb.com/?tab=series&id=${fragment.show!!.tvDbId}"), null, null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            this.fragmentReference.clear()
        }
    }

    companion object {
        private fun listToString(list: List<String>?): String {
            if (list == null || list.isEmpty()) {
                return ""
            }

            val builder = StringBuilder()

            list.forEachIndexed { i, string ->
                builder.append(string)

                if (i < list.size - 1) {
                    builder.append(", ")
                }
            }

            return builder.toString()
        }
    }
}