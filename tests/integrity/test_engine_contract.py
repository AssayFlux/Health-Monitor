import json
import math
import unittest

import health_engine
import observation_engine


class EngineContractTests(unittest.TestCase):
    def test_bmi_valid_and_invalid_inputs(self):
        self.assertEqual(health_engine.bmi(70, 175), 22.9)
        self.assertIsNone(health_engine.bmi(70, 0))
        self.assertIsNone(health_engine.bmi(None, 175))

    def test_lab_autocalculation_preserves_entered_values(self):
        values = {
            "total_cholesterol": 200,
            "hdl": 50,
            "triglycerides": 150,
            "ldl": 111,
        }
        result = health_engine.autocalc_labs(values)
        self.assertEqual(result["ldl"], 111)
        self.assertEqual(result["non_hdl"], 150)
        self.assertNotIn("vldl", result)
        estimated = health_engine.autocalc_labs(
            {"hdl": 50, "triglycerides": 150}
        )
        self.assertEqual(estimated["vldl"], 30)

    def test_lab_calculation_does_not_divide_by_zero(self):
        result = health_engine.autocalc_labs(
            {"total_cholesterol": 180, "hdl": 0, "triglycerides": 100}
        )
        self.assertNotIn("total_hdl_ratio", result)
        self.assertNotIn("ldl_hdl_ratio", result)

    def test_json_boundary_is_strict_json(self):
        payload = health_engine.calculate_vitals_json(
            {"weight_kg": 70, "height_cm": 175, "age": 30, "sex": "M"}
        )
        decoded = json.loads(
            payload,
            parse_constant=lambda value: (_ for _ in ()).throw(ValueError(value)),
        )
        self.assertIn("bmi", decoded)
        self.assertTrue(math.isfinite(decoded["bmi"]["value"]))

    def test_observation_normalization_rejects_invalid_enum(self):
        with self.assertRaises(ValueError):
            observation_engine.normalize_observation({"result_kind": "unknown-kind"})

    def test_observation_json_round_trip(self):
        payload = observation_engine.normalize_observation_json(
            {
                "parameter": "Heart rate",
                "result_kind": "quantitative",
                "value_numeric": 72,
                "unit": "bpm",
                "time_of_day": "morning",
            }
        )
        decoded = json.loads(payload)
        self.assertEqual(decoded["value_numeric"], 72.0)
        self.assertEqual(decoded["unit"], "bpm")


if __name__ == "__main__":
    unittest.main()
