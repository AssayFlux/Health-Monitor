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
    fun embeddedPythonAcceptsJsonAndReturnsFiniteVitalsJson() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        // JSON is the explicit cross-language contract: never pass java.util.Map
        // and rely on implicit conversion to a native Python dict.
        val input = JSONObject()
            .put("weight_kg", 70.0)
            .put("height_cm", 175.0)
            .put("age", 30)
            .put("sex", "M")

        val payload = Python.getInstance()
            .getModule("health_engine")
            .callAttr("calculate_vitals_json", input.toString())
            .toString()

        val result = JSONObject(payload)
        assertEquals(22.9, result.getJSONObject("bmi").getDouble("value"), 0.1)
        assertEquals("kcal/day", result.getJSONObject("bmr_kcal").getString("unit"))
    }
}
