"""Small supervised multinomial Naive Bayes classifier, not an LLM.

Training examples are intentionally small; output is only a draft category.
"""
import math
import re
from collections import Counter

TRAINING = {
    "diode": ["zener diode voltage regulator breakdown reverse leakage forward current cathode anode", "schottky barrier rectifier diode reverse voltage forward drop", "transient voltage suppressor avalanche diode clamping breakdown tvs", "light emitting diode led luminous intensity wavelength"],
    "resistor": ["resistor resistance tolerance temperature coefficient rated power thick film", "thin film chip resistor ohms resistance range tcr", "wirewound power resistor pulse load derating", "metal film precision resistor resistance tolerance"],
    "capacitor": ["capacitor capacitance rated voltage dielectric dissipation factor", "multilayer ceramic chip capacitors x7r c0g capacitance", "aluminum electrolytic capacitor ripple current impedance leakage", "film capacitor capacitance esr insulation resistance"],
    "inductor": ["power inductor inductance saturation current dc resistance shielded", "choke coil inductance rated current self resonant frequency", "smd wirewound inductor dcr isat irms", "ferrite power inductors inductance saturation temperature rise"],
}
LABELS = {"diode": "Діод", "resistor": "Резистор", "capacitor": "Конденсатор", "inductor": "Дросель"}

def tokens(text):
    return re.findall(r"[a-z]{2,}", text.lower())

class Classifier:
    def __init__(self):
        self.counts = {k: Counter(tokens(" ".join(v))) for k, v in TRAINING.items()}
        self.vocabulary = set().union(*(set(c) for c in self.counts.values()))

    def predict(self, text):
        words = [w for w in tokens(text) if w in self.vocabulary]
        if not words:
            return {"type": "unknown", "label": "Не визначено", "method": "multinomial-naive-bayes-v1", "unverified": True}
        scores = {}
        for key, counts in self.counts.items():
            total = sum(counts.values()) + len(self.vocabulary)
            scores[key] = sum(math.log((counts[w] + 1) / total) for w in words)
        winner = max(scores, key=scores.get)
        return {"type": winner, "label": LABELS[winner], "method": "multinomial-naive-bayes-v1", "unverified": True}
