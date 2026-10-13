import java.util.Locale
import kotlin.properties.Delegates

class InsufficientFundsException(message: String) : Exception(message)

infix fun Double.formattedAs(currency: String): String {
    val number = String.format(Locale.US, "%,.2f", this).replace(',', ' ')
    return "$number $currency"
}

fun Double.formatAsCurrency(): String = this formattedAs "NC"

interface PaymentMethod

class CardPayment(val last4Digits: String) : PaymentMethod

class CryptoPayment(val walletAddress: String) : PaymentMethod

class SmartWallet(val owner: String?, initialBalance: Double) {

    var balance: Double by Delegates.observable(initialBalance) { _, oldValue, newValue ->
        println("Лог ЛЯЛЯЛЯ: $oldValue -> $newValue")
    }

    val auditInfo: String by lazy {
        println("Формирование финансового отчета...")
        "Финансовый отчет: владелец - ${owner ?: "Анонимный пользователь"}, " +
                "баланс - ${balance.formatAsCurrency()}"
    }

    fun printOwnerInfo() {
        println("Владелец: ${owner?.uppercase() ?: "Анонимный пользователь"}")
    }

    operator fun plus(amount: Double): SmartWallet {
        balance += amount
        return this
    }

    operator fun minus(amount: Double): SmartWallet {
        if (amount < 0) {
            throw InsufficientFundsException("Нельзя снять отрицательную сумму: $amount")
        }
        if (amount > balance) {
            throw InsufficientFundsException(
                "Недостаточно средств: запрошено $amount, на счете $balance"
            )
        }
        balance -= amount
        return this
    }

    fun processPayment(method: PaymentMethod) {
        if (method is CardPayment) {
            val card = method as? CardPayment
            println("Оплата картой, последние 4 цифры: ${card?.last4Digits}")
        } else if (method is CryptoPayment) {
            val crypto = method as? CryptoPayment
            println("Оплата криптовалютой, адрес: ${crypto?.walletAddress}")
        } else {
            println("Неизвестное платежное средство")
        }
    }
}

class WalletBuilder {
    var owner: String? = null
    var initialBalance: Double = 0.0
}

fun createWallet(block: WalletBuilder.() -> Unit): SmartWallet {
    val builder = WalletBuilder()
    builder.block()
    return SmartWallet(builder.owner, builder.initialBalance)
}

fun main() {
    println("--- Шаг 1: создание кошелька (createWallet, apply, also) ---")
    val wallet = createWallet {
        owner = "Иван Петров"
        initialBalance = 1000.0
    }.apply {
        printOwnerInfo()
    }.also {
        println("Кошелек создан, баланс: ${it.balance.formatAsCurrency()}")
    }

    println("--- Шаг 2: оператор plus (+500.0) ---")
    wallet + 500.0

    println("--- Шаг 3: let и имя владельца ---")
    wallet.owner?.let {
        println("Имя владельца проверено: $it")
    }

    println("--- Шаг 4: оператор minus (-2000.0) ---")
    try {
        wallet - 2000.0
    } catch (e: InsufficientFundsException) {
        println("Ошибка: ${e.message}")
    }

    println("--- Шаг 5: CryptoPayment, is и as? ---")
    val payment: PaymentMethod = CryptoPayment("0xA1B2C3D4E5F6")
    if (payment is CryptoPayment) {
        val crypto = payment as? CryptoPayment
        println("Адрес криптокошелька: ${crypto?.walletAddress}")
    }

    println("--- Шаг 6: lazy (auditInfo) ---")
    println(wallet.auditInfo)

    println("--- Шаг 7: инфиксная нотация ---")
    println(750.5 formattedAs "NC")
}