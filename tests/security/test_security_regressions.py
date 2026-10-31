import math
import unittest
from pathlib import Path

import health_engine
import observation_engine


class SecurityRegressionTests(unittest.TestCase):

    def test_assistant_voice_path_is_strictly_on_device(self):
        root = Path(__file__).resolve().parents[2]
        source = (
            root
            / "app/src/main/java/com/assayflux/healthmonitor/OfflineAssistantScreen.kt"
        ).read_text()
        self.assertIn("SpeechRecognizer.createOnDeviceSpeechRecognizer", source)
        self.assertNotIn("SpeechRecognizer.createSpeechRecognizer(", source)
        self.assertIn("!it.isNetworkConnectionRequired", source)
        self.assertIn("var useRecords by remember { mutableStateOf(false) }", source)
        self.assertIn("Llama.complete(model = model", source)
        self.assertNotIn("OkHttpClient", source)
        self.assertNotIn("HttpURLConnection", source)

    def test_non_finite_measurements_are_not_serialized(self):
        result = health_engine.summarize_results(
            [
                {"value": float("nan")},
                {"value": float("inf")},
                {"value": 4},
            ]
        )
        self.assertEqual(result["count"], 1)
        self.assertEqual(result["latest"], 4)

    def test_invalid_numeric_text_does_not_crash_engine(self):
        self.assertIsNone(health_engine.bmi("not-a-number", 170))
        self.assertIsNone(health_engine.bmi(70, "bad-height"))

    def test_invalid_observation_values_become_absent_with_reason(self):
        result = observation_engine.normalize_observation(
            {
                "parameter": "Glucose",
                "result_kind": "quantitative",
                "value_numeric": "not numeric",
                "absent_reason": "not_recorded",
            }
        )
        self.assertIsNone(result["value_numeric"])
        self.assertEqual(result["absent_reason"], "not_recorded")

    def test_invalid_absent_reason_is_rejected(self):
        with self.assertRaises(ValueError):
            observation_engine.normalize_observation(
                {
                    "result_kind": "quantitative",
                    "value_numeric": None,
                    "absent_reason": "execute-code",
                }
            )

    def test_non_finite_numeric_is_not_accepted_as_measurement(self):
        result = observation_engine.normalize_observation(
            {
                "result_kind": "quantitative",
                "value_numeric": math.inf,
                "absent_reason": "not_recorded",
            }
        )
        self.assertIsNone(result["value_numeric"])


if __name__ == "__main__":
    unittest.main()
