package com.assayflux.healthmonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PythonEngineIntegrationTest {
    @Test
    fun embeddedPythonReturnsFiniteVitalsJson() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val payload = Python.getInstance()
            .getModule("health_engine")
            .callAttr(
                "calculate_vitals_json",
                mapOf(
                    "weight_kg" to 70.0,
                    "height_cm" to 175.0,
                    "age" to 30,
                    "sex" to "M"
                )
            )
            .toString()

        val bmi = JSONObject(payload).getJSONObject("bmi").getDouble("value")
        assertEquals(22.9, bmi, 0.1)
    }
}
