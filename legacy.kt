// Legacy.kt
// Extremely insecure legacy payment system in Kotlin
// Educational bad code example - full of SQL injection, plain text secrets, massive code duplication

import java.sql.*
import java.io.File
import java.time.LocalDateTime
import org.postgresql.Driver

object Legacy {

    private const val DB_HOST = "localhost"
    private const val DB_PORT = "5432"
    private const val DB_NAME = "payment_legacy_db"
    private const val DB_USER = "postgres"
    private const val DB_PASS = "SuperSecret123!"
    private const val SITE_SECRET = "myglobalsecret123"

    private var GLOBAL_CONN: Connection? = null

    private fun getConnection(): Connection {
        if (GLOBAL_CONN == null || GLOBAL_CONN!!.isClosed) {
            val url = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
            GLOBAL_CONN = DriverManager.getConnection(url, DB_USER, DB_PASS)
            GLOBAL_CONN!!.createStatement().execute("SET client_encoding = 'UTF8';")
        }
        return GLOBAL_CONN!!
    }

    private fun appendToLog(msg: String) {
        try {
            File("legacy_errors.log").appendText("${LocalDateTime.now()} | $msg\n")
        } catch (e: Exception) {
            // silent fail - classic legacy
        }
    }

    fun register_customer(username: String, email: String, password: String, full_name: String,
                          phone: String = "", country: String = "RS", city: String = "", address: String = "") {
        try {
            val conn = getConnection()
            val id = "cust_${System.currentTimeMillis()}"
            val sql = """
                INSERT INTO customers (
                    id, username, email, password, full_name, phone, country, city, address_line_1,
                    created_at, updated_at, register_ip, user_agent, is_admin, role_name
                ) VALUES (
                    '$id', '$username', '$email', '$password', '$full_name', '$phone', '$country', '$city', '$address',
                    NOW()::text, NOW()::text, '127.0.0.1', 'KOTLIN-LEGACY', 'false', 'customer'
                ) RETURNING id;
            """.trimIndent()

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            if (rs.next()) {
                println("Customer registered ID: ${rs.getString("id")}")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun login_customer(username: String, password: String) {
        try {
            val conn = getConnection()
            val sql = "SELECT * FROM customers WHERE username = '$username' AND password = '$password' LIMIT 1;"

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            if (rs.next()) {
                val id = rs.getString("id")
                val session_token = System.currentTimeMillis().toString()
                val update = """
                    UPDATE customers SET session_token = '$session_token', 
                    last_login_ip = '127.0.0.1', failed_login_count = '0', 
                    updated_at = NOW()::text WHERE id = '$id';
                """.trimIndent()
                stmt.executeUpdate(update)
                println("LOGIN SUCCESS Session: $session_token")
            } else {
                val failSql = "UPDATE customers SET failed_login_count = (failed_login_count::int + 1)::text WHERE username = '$username';"
                stmt.executeUpdate(failSql)
                println("LOGIN FAILED")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun get_customer(customer_id: String) {
        try {
            val conn = getConnection()
            val sql = "SELECT * FROM customers WHERE id = '$customer_id' LIMIT 1;"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            if (rs.next()) {
                println("Customer found: ${rs.getString("username")}")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun update_customer_profile(customer_id: String, new_email: String, new_phone: String, new_address: String) {
        try {
            val conn = getConnection()
            val sql = "UPDATE customers SET email = '$new_email', phone = '$new_phone', " +
                      "address_line_1 = '$new_address', updated_at = NOW()::text WHERE id = '$customer_id';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Customer profile updated")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun reset_password(email: String, new_password: String) {
        try {
            val conn = getConnection()
            val sql = "UPDATE customers SET password = '$new_password', " +
                      "reset_token = 'reset_' || md5(NOW()::text), " +
                      "reset_token_expires_at = (NOW() + INTERVAL '1 day')::text WHERE email = '$email';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Password reset token generated for $email")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun verify_email(token: String) {
        try {
            val conn = getConnection()
            val sql = "UPDATE customers SET email_verification_token = NULL WHERE email_verification_token = '$token';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Email verified with token $token")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun add_payment_method(customer_id: String, type: String, card_number: String,
                           expiry_month: String, expiry_year: String, cvv: String,
                           holder_name: String, iban: String = "") {
        try {
            val conn = getConnection()
            val id = "pm_${System.currentTimeMillis()}"
            val sql = """
                INSERT INTO payment_methods (
                    id, customer_id, type, provider, card_number, card_expiry_month, card_expiry_year,
                    card_cvv, card_holder_name, iban, active_flag, created_at, updated_at
                ) VALUES (
                    '$id', '$customer_id', '$type', 'legacy_bank_gateway', '$card_number', '$expiry_month',
                    '$expiry_year', '$cvv', '$holder_name', '$iban', 'true', NOW()::text, NOW()::text
                ) RETURNING id;
            """.trimIndent()

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            if (rs.next()) {
                println("Payment method added ID: ${rs.getString("id")}")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun process_payment(customer_id: String, payment_method_id: String, amount: String,
                        currency: String = "EUR", external_order_id: String? = null, ip: String? = null) {
        try {
            val conn = getConnection()
            val id = "pay_${System.currentTimeMillis()}"
            val realIp = ip ?: "127.0.0.1"
            val extOrder = external_order_id ?: "ord_${System.currentTimeMillis()}"
            val rawPayload = """{"card_number":"****4242","provider_secret":"sk_live_9876543210abcdef","cvv_used":"123","3ds_password":"customer123"}"""

            val sql = """
                INSERT INTO payments (
                    id, customer_id, payment_method_id, external_order_id, amount, currency, status,
                    provider_ref, ip_address, raw_provider_payload, created_at, paid_at, captured_flag
                ) VALUES (
                    '$id', '$customer_id', '$payment_method_id', '$extOrder', '$amount', '$currency', 'captured',
                    'prov_${System.currentTimeMillis()}', '$realIp', '$rawPayload', NOW()::text, NOW()::text, 'true'
                ) RETURNING id;
            """.trimIndent()

            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            if (rs.next()) {
                val payId = rs.getString("id")
                val update = "UPDATE customers SET total_paid = (COALESCE(total_paid::numeric, 0) + $amount)::text WHERE id = '$customer_id';"
                stmt.executeUpdate(update)
                println("PAYMENT PROCESSED ID: $payId Amount: $amount $currency")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun list_payments(customer_id: String) {
        try {
            val conn = getConnection()
            val sql = "SELECT * FROM payments WHERE customer_id = '$customer_id' ORDER BY created_at DESC;"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(sql)
            var count = 0
            while (rs.next()) count++
            println("Listed $count payments for customer")
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun create_refund(payment_id: String, amount: String, reason: String = "customer request") {
        try {
            val conn = getConnection()
            val id = "ref_${System.currentTimeMillis()}"
            val sql = "INSERT INTO refunds (id, payment_id, amount, currency, status, reason, created_at) " +
                      "VALUES ('$id', '$payment_id', '$amount', 'EUR', 'pending', '$reason', NOW()::text);"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Refund created for payment $payment_id")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun process_refund(refund_id: String) {
        try {
            val conn = getConnection()
            val sql = "UPDATE refunds SET status = 'processed', processed_at = NOW()::text WHERE id = '$refund_id';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Refund processed ID: $refund_id")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun simulate_chargeback(payment_id: String, amount: String, reason: String = "fraud") {
        try {
            val conn = getConnection()
            val id = "cb_${System.currentTimeMillis()}"
            val sql = "INSERT INTO chargebacks (id, payment_id, amount, currency, reason, status, created_at, deadline_at) " +
                      "VALUES ('$id', '$payment_id', '$amount', 'EUR', '$reason', 'open', NOW()::text, (NOW() + INTERVAL '7 days')::text);"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Chargeback created for payment $payment_id")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun resolve_chargeback(chargeback_id: String, won: String = "true") {
        try {
            val conn = getConnection()
            val sql = "UPDATE chargebacks SET status = 'closed', won_flag = '$won', closed_at = NOW()::text WHERE id = '$chargeback_id';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Chargeback resolved ID: $chargeback_id")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun create_fraud_review(payment_id: String, customer_id: String, score: String = "85") {
        try {
            val conn = getConnection()
            val id = "fraud_${System.currentTimeMillis()}"
            val sql = "INSERT INTO fraud_reviews (id, payment_id, customer_id, score, decision, created_at) " +
                      "VALUES ('$id', '$payment_id', '$customer_id', '$score', 'pending', NOW()::text);"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Fraud review created for payment $payment_id")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun decide_fraud_review(review_id: String, decision: String, reviewer_email: String, reviewer_password: String) {
        try {
            val conn = getConnection()
            val check = "SELECT * FROM customers WHERE email = '$reviewer_email' AND password = '$reviewer_password' AND is_admin = 'true';"
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery(check)
            if (rs.next()) {
                val sql = "UPDATE fraud_reviews SET decision = '$decision', reviewer = '$reviewer_email', updated_at = NOW()::text WHERE id = '$review_id';"
                stmt.executeUpdate(sql)
                println("Fraud review decided as $decision")
            } else {
                println("Fraud review access denied")
            }
            rs.close()
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun admin_export_all_data() {
        try {
            val conn = getConnection()
            val sql = """
                COPY (
                    SELECT * FROM customers 
                    UNION ALL SELECT * FROM payments 
                    UNION ALL SELECT * FROM payment_methods 
                    UNION ALL SELECT * FROM refunds 
                    UNION ALL SELECT * FROM chargebacks 
                    UNION ALL SELECT * FROM fraud_reviews
                ) TO '/tmp/legacy_full_export_${System.currentTimeMillis()}.csv' WITH CSV HEADER;
            """.trimIndent()
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Full data export completed to /tmp/legacy_full_export_*.csv")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun ban_customer(customer_id: String) {
        try {
            val conn = getConnection()
            val sql = "UPDATE customers SET blocked_flag = 'true' WHERE id = '$customer_id';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("Customer banned")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    fun generate_api_key(customer_id: String) {
        try {
            val conn = getConnection()
            val key = "key_${System.currentTimeMillis()}"
            val secret = "secret_${System.currentTimeMillis() * 2}"
            val sql = "UPDATE customers SET api_key = '$key', api_secret = '$secret' WHERE id = '$customer_id';"
            val stmt = conn.createStatement()
            stmt.executeUpdate(sql)
            println("API key generated: $key")
            stmt.close()
        } catch (e: Exception) {
            println("[ERROR] ${e.message}")
            appendToLog("${e.message}")
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("LEGACY PAYMENT SYSTEM STARTED (Kotlin version)")

        register_customer("testuser1", "test1@example.com", "PlainPass123", "Test User One", "381601234567", "RS", "Belgrade", "Novi Beograd 1")
        register_customer("testuser2", "test2@example.com", "AnotherPass456", "Test User Two", "381609876543", "RS", "Novi Sad", "Address 2")

        login_customer("testuser1", "PlainPass123")
        login_customer("testuser2", "AnotherPass456")

        add_payment_method("cust_...", "card", "4242424242424242", "12", "2028", "123", "Test User One")
        add_payment_method("cust_...", "iban", "", "", "", "", "Test User Two", "RS12345678901234567890")

        process_payment("cust_...", "pm_...", "149.99", "EUR", "ORDER-1001")
        process_payment("cust_...", "pm_...", "299.50", "USD", "ORDER-1002")

        create_refund("pay_...", "49.99", "partial return")
        process_refund("ref_...")

        simulate_chargeback("pay_...", "299.50", "dispute")
        resolve_chargeback("cb_...", "false")

        create_fraud_review("pay_...", "cust_...", "78")
        decide_fraud_review("fraud_...", "approve", "admin@legacy.com", "AdminPass123")

        reset_password("test1@example.com", "NewPlainPass789")
        verify_email("email_verify_token_demo")

        admin_export_all_data()

        ban_customer("cust_...")
        generate_api_key("cust_...")

        println("LEGACY PAYMENT SYSTEM WORKFLOW COMPLETE")
    }
}