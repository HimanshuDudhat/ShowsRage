package com.mgaetan89.showsrage.extension

import android.content.SharedPreferences
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.mgaetan89.showsrage.TestActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesExtension_GetVersionCheckIntervalTest {
    @JvmField
    @Rule
    val activityRule: ActivityTestRule<TestActivity> = ActivityTestRule(TestActivity::class.java)

    private lateinit var preference: SharedPreferences

    @Before
    fun before() {
        this.preference = this.activityRule.activity.getPreferences()
    }

    @Test
    fun getVersionCheckInterval() {
        this.preference.edit().putString(Fields.VERSION_CHECK_INTERVAL.field, "86400000").apply()

        val versionCheckInterval = this.preference.getVersionCheckInterval()

        assertThat(versionCheckInterval).isEqualTo(86400000L)
    }

    @Test
    fun getVersionCheckInterval_Empty() {
        this.preference.edit().putString(Fields.VERSION_CHECK_INTERVAL.field, "").apply()

        val versionCheckInterval = this.preference.getVersionCheckInterval()

        assertThat(versionCheckInterval).isEqualTo(0L)
    }

    @Test
    fun getVersionCheckInterval_Missing() {
        assertThat(this.preference.contains(Fields.VERSION_CHECK_INTERVAL.field)).isFalse()

        val versionCheckInterval = this.preference.getVersionCheckInterval()

        assertThat(versionCheckInterval).isEqualTo(0L)
    }

    @Test
    fun getVersionCheckInterval_Null() {
        this.preference.edit().putString(Fields.VERSION_CHECK_INTERVAL.field, null).apply()

        val versionCheckInterval = this.preference.getVersionCheckInterval()

        assertThat(versionCheckInterval).isEqualTo(0L)
    }

    @After
    fun after() {
        this.preference.edit().clear().apply()
    }
}
