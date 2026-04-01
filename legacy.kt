fun register_customer(username: String, email: String, password: String, full_name: String, phone: String = "", country: String = "RS", city: String = "", address: String = "") {
}

fun login_customer(username: String, password: String) {
}

fun get_customer(customer_id: String) {
}

fun update_customer_profile(customer_id: String, new_email: String, new_phone: String, new_address: String) {
}

fun reset_password(email: String, new_password: String) {
}

fun verify_email(token: String) {
}

fun add_payment_method(customer_id: String, type: String, card_number: String, expiry_month: String, expiry_year: String, cvv: String, holder_name: String, iban: String = "") {
}

fun list_payment_methods(customer_id: String) {
}

fun delete_payment_method(pm_id: String) {
}

fun process_payment(customer_id: String, payment_method_id: String, amount: String, currency: String = "EUR", external_order_id: String? = null, ip: String? = null) {
}

fun list_payments(customer_id: String) {
}

fun get_payment_details(payment_id: String) {
}

fun create_refund(payment_id: String, amount: String, reason: String = "customer request") {
}

fun process_refund(refund_id: String) {
}

fun simulate_chargeback(payment_id: String, amount: String, reason: String = "fraud") {
}

fun resolve_chargeback(chargeback_id: String, won: String = "true") {
}

fun create_fraud_review(payment_id: String, customer_id: String, score: String = "85") {
}

fun decide_fraud_review(review_id: String, decision: String, reviewer_email: String, reviewer_password: String) {
}

fun admin_list_all_customers() {
}

fun admin_export_all_data() {
}

fun search_payments(search_term: String) {
}

fun process_recurring_billing() {
}

fun handle_webhook(payload: String) {
}

fun ban_customer(customer_id: String) {
}

fun generate_api_key(customer_id: String) {
}
