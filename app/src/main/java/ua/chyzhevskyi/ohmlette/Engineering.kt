package ua.chyzhevskyi.ohmlette

import java.util.Locale
import kotlin.math.*

object Engineering {
    val prefixes = linkedMapOf("п" to -12, "н" to -9, "мк" to -6, "м" to -3, "без префікса" to 0, "к" to 3, "М" to 6, "Г" to 9)
    fun parse(text: String): Double {
        val match = Regex("^([+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)(?:[eE][+-]?\\d+)?)([pnumkMGµμ]?)$").matchEntire(text.trim())
            ?: error("Введіть число, наприклад 4,7 або 4.7k")
        val exponent = mapOf("" to 0, "p" to -12, "n" to -9, "u" to -6, "µ" to -6, "μ" to -6, "m" to -3, "k" to 3, "M" to 6, "G" to 9).getValue(match.groupValues[2])
        return (match.groupValues[1].replace(',', '.').toDouble() * 10.0.pow(exponent)).also { require(it.isFinite()) { "Число завелике" } }
    }
    fun convert(text: String, from: Int, to: Int): Double = (parse(text) * 10.0.pow(from - to)).also {
        require(it.isFinite()) { "Результат завеликий" }
    }
    fun format(value: Double) = String.format(Locale.US, "%.8g", value)
    data class Nominal(val resistance: Double, val errorPercent: Double)
    private val e12 = listOf(10,12,15,18,22,27,33,39,47,56,68,82)
    private val e24 = listOf(10,11,12,13,15,16,18,20,22,24,27,30,33,36,39,43,47,51,56,62,68,75,82,91)
    private val e96 = listOf(100,102,105,107,110,113,115,118,121,124,127,130,133,137,140,143,147,150,154,158,162,165,169,174,178,182,187,191,196,200,205,210,215,221,226,232,237,243,249,255,261,267,274,280,287,294,301,309,316,324,332,340,348,357,365,374,383,392,402,412,422,432,442,453,464,475,487,499,511,523,536,549,562,576,590,604,619,634,649,665,681,698,715,732,750,768,787,806,825,845,866,887,909,931,953,976)
    fun nearest(target: Double, series: Int): List<Nominal> {
        require(target.isFinite() && target in 1e-9..1e15) { "Опір має бути від 1 нОм до 1 ПОм" }
        val base = when(series) { 12 -> e12.map { it / 10.0 }; 24 -> e24.map { it / 10.0 }; 96 -> e96.map { it / 100.0 }; else -> error("Невідомий ряд") }
        val decade = floor(log10(target)).toInt()
        return (decade-1..decade+1).flatMap { d -> base.map { it * 10.0.pow(d) } }
            .sortedBy { abs(it-target) }.take(3).map { Nominal(it, (it-target)/target*100) }
    }
}
