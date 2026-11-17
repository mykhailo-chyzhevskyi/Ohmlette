import unittest
from classifier import Classifier
from server import make_card, validate_url

class BackendTests(unittest.TestCase):
    def test_classifier_unseen_phrases(self):
        model = Classifier()
        for phrase, expected in [("Zener voltage regulators SOT23", "diode"), ("precision chip resistance tolerance", "resistor"), ("ceramic dielectric capacitance", "capacitor"), ("shielded coil inductance saturation", "inductor")]:
            self.assertEqual(expected, model.predict(phrase)["type"])

    def test_no_vocabulary(self):
        self.assertEqual("unknown", Classifier().predict("xyz123")["type"])

    def test_bad_pdf(self):
        with self.assertRaises(ValueError): make_card(b"bad", "BZX84")

    def test_private_url(self):
        with self.assertRaises(ValueError): validate_url("https://127.0.0.1/private")

if __name__ == "__main__": unittest.main()
