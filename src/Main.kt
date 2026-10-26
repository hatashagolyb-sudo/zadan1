import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*

// ============================================================
// Модель данных
// ============================================================
data class SensorReading(
    val sensorId: String,
    val value: Double,
    val timestamp: Long
)

// ============================================================
// Корутины: параллельная генерация событий
// ============================================================
suspend fun emitSensorData(channel: SendChannel<SensorReading>) {
    val sensors = listOf("TEMP", "HUM", "CO2")
    coroutineScope {
        for (sensorId in sensors) {
            launch {
                for (i in 0..9) {
                    delay(500L)
                    val reading = SensorReading(sensorId, 15.0 + i * 2.0, System.currentTimeMillis())
                    channel.send(reading)
                }
            }
        }
    }
    channel.close()
}

// ============================================================
// Flow: ленивый поток
// ============================================================
fun sensorFlow(sensorId: String): Flow<SensorReading> = flow {
    println("Поток $sensorId запущен")
    for (i in 0..9) {
        delay(300L)
        emit(SensorReading(sensorId, 15.0 + i * 2.0, System.currentTimeMillis()))
    }
}

// ============================================================
// main: сравнение двух подходов
// ============================================================
suspend fun main() = coroutineScope<Unit> {

    println("=== Часть 1: каналы и корутины (3 датчика, 30 показаний) ===")
    val channel = Channel<SensorReading>()
    launch { emitSensorData(channel) }
    var count = 0
    for (msg in channel) {
        count++
        println("Получено из канала №$count: $msg")
    }
    println("Всего получено показаний: $count")

    println("=== Часть 2: Flow (ленивость, цепочка операций) ===")
    val tempFlow = sensorFlow("TEMP")
        .filter { it.value > 20.0 }
        .map { it.copy(value = it.value * 1.1) }
        .take(5)
        .onEach { println("Обработано: $it") }
    println("Цепочка операций собрана, но поток ещё молчит")
    println("Вызываем терминальную операцию toList()")
    val readings = tempFlow.toList()
    println("Результат toList(): $readings")

    println("=== Часть 3: fold - сумма и среднее ===")
    val total = tempFlow.fold(Pair(0.0, 0)) { acc, reading ->
        Pair(acc.first + reading.value, acc.second + 1)
    }
    val average = total.first / total.second
    println("Сумма: ${total.first}, количество: ${total.second}, среднее: $average")
}