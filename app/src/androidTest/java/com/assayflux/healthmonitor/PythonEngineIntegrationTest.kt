package com.assayflux.healthmonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaquo.python.AndroidPlatform
import com.chaquo.python.Python
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

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
                mapOf("weight_kg" to 70, "height_cm" to 175, "age" to 30, "sex" to "M")
            )
            .toString()

        val bmi = JSONObject(payload).getJSONObject("bmi").getDouble("value")
        assertEquals(22.9, bmi, 0.1)
    }
}
