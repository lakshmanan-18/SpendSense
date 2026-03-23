package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.*

val MONTHS = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

data class Expense(
    val amount: Double,
    val currency: String = "Rs",
    val description: String,
    val timestamp: Long,
    val category: String,
    val isGoodSpend: Boolean? = null,
    val suggestion: String? = null
) {
    val dateString: String
        get() = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

object ExpenseRepository {
    private val _expensesFlow = MutableSharedFlow<Expense>(extraBufferCapacity = 1)
    val expensesFlow = _expensesFlow.asSharedFlow()

    fun addExpense(expense: Expense) {
        _expensesFlow.tryEmit(expense)
    }

    fun analyzeSpend(description: String, category: String): Pair<Boolean?, String?> {
        if (category == "Income" || category == "Refund") return Pair(true, if (category == "Refund") "Money back! Great for your savings." else null)
        val t = description.lowercase()
        
        val dataset = listOf(
            Triple(listOf("chai", "tea", "cutting"), true, "Affordable pleasure; fine in moderation"),
            Triple(listOf("coffee", "cappuccino", "latte", "starbucks"), null, "Can add up; home brew saves money"),
            Triple(listOf("kirana", "grocery", "supermarket", "bigbasket", "dmart", "reliance"), true, "Core necessity; bulk buying saves money"),
            Triple(listOf("vegetables", "sabzi", "dal", "atta", "rice", "zepto"), true, "Healthiest & most economical option"),
            Triple(listOf("pani puri", "vada pav", "dosa", "bhel"), null, "Fine occasionally; hygiene risk if frequent"),
            Triple(listOf("restaurant", "dine", "zomato pay", "swiggy pay"), null, "Treat it as occasional; budget mindfully"),
            Triple(listOf("zomato", "swiggy", "delivery"), false, "Delivery fees + surge = expensive habit"),
            Triple(listOf("mcdonald", "kfc", "burger king", "dominos", "pizza"), null, "Occasional is fine; unhealthy if frequent"),
            Triple(listOf("chips", "biscuit", "kurkure", "lays"), false, "High cost per calorie; low nutrition"),
            Triple(listOf("fruit", "apple", "banana", "cashew", "almond"), true, "Nutritional investment worth making"),
            Triple(listOf("auto", "rickshaw"), true, "Economical local commute"),
            Triple(listOf("bus", "ksrtc", "tnstc", "gsrtc"), true, "Most economical public transport"),
            Triple(listOf("metro", "local train", "dmrc"), true, "Efficient city commute; saves time & fuel"),
            Triple(listOf("ola", "uber", "cab", "ride"), null, "Comfortable but costly; use wisely"),
            Triple(listOf("rapido", "bike taxi"), null, "Cheaper than cabs; good for solo trips"),
            Triple(listOf("petrol", "fuel", "diesel", "hp", "ioc", "bpcl"), true, "Necessary if you own a vehicle"),
            Triple(listOf("service", "tyre", "repair", "motor"), true, "Prevents costly breakdowns later"),
            Triple(listOf("irctc", "train ticket", "railway"), true, "Affordable intercity travel"),
            Triple(listOf("indigo", "air india", "spicejet", "flight"), null, "Plan ahead for cheap fares"),
            Triple(listOf("parking"), null, "Unavoidable with a vehicle"),
            Triple(listOf("electricity", "bescom", "msedcl", "tneb", "bill"), true, "Pay on time to avoid penalties"),
            Triple(listOf("water bill", "corporation"), true, "Essential utility"),
            Triple(listOf("jio", "airtel", "vi", "recharge", "postpaid"), true, "Essential; compare plans regularly"),
            Triple(listOf("broadband", "wifi"), true, "Needed for WFH/study; compare ISPs"),
            Triple(listOf("lpg", "gas", "indane", "bharat gas"), true, "Essential cooking fuel"),
            Triple(listOf("rent", "house rent", "pg", "hostel"), true, "Keep rent under 30% of income"),
            Triple(listOf("maintenance", "society", "apartment"), true, "Mandatory for apartment dwellers"),
            Triple(listOf("myntra", "ajio", "zara", "clothing", "shirt", "dress"), null, "Buy seasonal; avoid impulse buys"),
            Triple(listOf("shoes", "sandals", "bata", "nike", "puma"), null, "Quality over quantity"),
            Triple(listOf("amazon", "flipkart", "croma", "electronics"), null, "Research before buying; avoid EMI traps"),
            Triple(listOf("oneplus", "samsung", "iphone", "redmi", "mobile"), null, "Replace only when necessary"),
            Triple(listOf("refrigerator", "washing machine", "ac", "geyser"), true, "Invest in energy-efficient models"),
            Triple(listOf("notebook", "pen", "book", "stationery"), true, "Invest in learning tools"),
            Triple(listOf("novel", "textbook"), true, "ROI on knowledge is infinite"),
            Triple(listOf("nykaa", "beauty", "shampoo", "skincare", "loreal"), true, "Essentials are fine; luxury brands optional"),
            Triple(listOf("gift", "diwali"), null, "Plan a gifting budget to avoid overspend"),
            Triple(listOf("sale", "deal", "meesho"), false, "Unplanned spending; kills savings fast"),
            Triple(listOf("doctor", "consultation", "clinic", "hospital"), true, "Never skip needed healthcare"),
            Triple(listOf("medicine", "pharmacy", "medplus", "apollo"), true, "Buy generics where possible"),
            Triple(listOf("health insurance", "star health", "niva bupa"), true, "Critical protection; buy young for low premium"),
            Triple(listOf("gym", "fitness", "cult"), true, "Investment in long-term health"),
            Triple(listOf("yoga", "zumba"), true, "Excellent mind-body investment"),
            Triple(listOf("lab", "diagnostic", "thyrocare", "dr lal"), true, "Don't delay health checkups"),
            Triple(listOf("whey", "protein", "supplement"), null, "Useful for gym-goers; not universally needed"),
            Triple(listOf("therapy", "counselling", "yourdost"), true, "Destigmatize and prioritize mental health"),
            Triple(listOf("fees", "tuition", "college", "university"), true, "Core investment in your future"),
            Triple(listOf("udemy", "coursera", "nptel", "edx", "course"), true, "Great ROI for career growth"),
            Triple(listOf("coaching", "iit", "neet"), true, "Targeted exam preparation"),
            Triple(listOf("exam fee", "registration", "gate", "gre"), true, "Necessary for certifications"),
            Triple(listOf("netflix", "prime", "hotstar", "jiocinema", "sonyliv", "zee5"), null, "Cap at 1-2 subscriptions; share family plan"),
            Triple(listOf("pvr", "inox", "bookmyshow", "cinema", "movie"), null, "Occasional fun; use discount apps"),
            Triple(listOf("bgmi", "freefire", "google play", "game"), false, "In-app purchases drain money fast"),
            Triple(listOf("concert", "event", "show"), null, "Occasional cultural experiences are fine"),
            Triple(listOf("wonderla", "amusement", "theme park"), null, "Annual family outing; budget for it"),
            Triple(listOf("bar", "club", "beer", "alcohol", "pub"), false, "High cost, often peer-pressured spending"),
            Triple(listOf("art", "music class", "guitar", "hobby"), true, "Enriches life; manageable cost"),
            Triple(listOf("cricket bat", "badminton", "racket", "sport"), true, "One-time cost; promotes active lifestyle"),
            Triple(listOf("sip", "mutual fund", "zerodha", "groww"), true, "Best habit for long-term wealth creation"),
            Triple(listOf("fd", "fixed deposit", "sbi", "hdfc"), true, "Low risk; good for emergency corpus"),
            Triple(listOf("rd", "recurring deposit"), true, "Disciplined saving habit"),
            Triple(listOf("ppf", "epf", "provident fund"), true, "Tax-free long-term retirement corpus"),
            Triple(listOf("gold", "digital gold", "sovereign bond"), true, "Hedge against inflation; avoid excess"),
            Triple(listOf("stocks", "shares", "upstox"), true, "High risk-reward; learn before investing"),
            Triple(listOf("savings", "emergency", "liquid"), true, "3-6 months expenses in liquid savings"),
            Triple(listOf("atm", "cash withdrawal"), null, "Minimize to avoid fees; use UPI"),
            Triple(listOf("bank charge", "penalty"), false, "Maintain min balance; avoid penalty fees"),
            Triple(listOf("credit card", "payment"), true, "Always pay FULL bill to avoid interest"),
            Triple(listOf("emi", "loan", "bajaj"), true, "Keep total EMIs under 40% of income"),
            Triple(listOf("upi", "neft", "phonepe", "gpay", "paytm", "transfer"), true, "Free and instant; track who you pay"),
            Triple(listOf("lic", "term insurance"), true, "Term insurance is a must-have"),
            Triple(listOf("membership"), null, "Only worth it if you order frequently"),
            Triple(listOf("salon", "haircut", "parlour", "grooming"), true, "Basic grooming is a necessity"),
            Triple(listOf("laundry", "drycleaning", "wash"), true, "Essential chore; doorstep pickup is value"),
            Triple(listOf("newspaper", "magazine"), true, "Informed citizens make better decisions"),
            Triple(listOf("donation", "temple", "charity", "ngo"), true, "Positive social impact; budget a fixed %"),
            Triple(listOf("party", "celebration", "birthday"), null, "Plan a social budget; FOMO spending hurts"),
            Triple(listOf("pan", "gutkha", "cigarette", "tobacco", "bidi"), false, "Health & financial damage; quit if possible"),
            Triple(listOf("lottery", "satta", "bet", "fantasy", "dream11"), false, "Negative expected value; avoid entirely"),
            Triple(listOf("cash", "misc", "unknown"), false, "Untracked cash bleeds savings silently")
        )

        for (item in dataset) {
            if (item.first.any { t.contains(it) }) {
                return Pair(item.second, item.third)
            }
        }

        return when (category) {
            "Bills", "Rent", "Health", "Education" -> Pair(true, "Essential spending. Good job staying on top of priorities.")
            "Shopping" -> Pair(null, "Try to stick to a shopping list to avoid impulse purchases.")
            "Food" -> Pair(true, "Eating is essential, but watch out for frequent restaurant visits.")
            else -> Pair(null, "Track your spending patterns to optimize your savings.")
        }
    }

    fun categorize(text: String): String {
        val t = text.lowercase()
        if (t.contains("refund")) return "Refund"
        val incomeKeywords = listOf("credited", "received", "deposit", "cashback", "salary", "added", "reversal")
        val foodKeywords = listOf("swiggy", "zomato", "eatclub", "dominos", "pizza", "kfc", "mcdonald", "restaurant", "food", "cafe", "hotel", "burger", "chai", "tea", "coffee", "kirana", "grocery", "vegetables")
        val travelKeywords = listOf("uber", "ola", "rapido", "metro", "bus", "irctc", "railway", "flight", "airlines", "fuel", "petrol", "diesel", "fastag", "toll", "auto", "rickshaw")
        val shoppingKeywords = listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "snapdeal", "store", "mart", "supermarket", "reliance", "dmart", "clothing", "shoes", "electronics", "mobile", "nykaa")
        val billsKeywords = listOf("electricity", "eb", "water", "gas", "broadband", "wifi", "bill", "postpaid", "prepaid", "recharge", "airtel", "jio", "bsnl", "vi", "bescom", "msedcl", "tneb")
        val healthKeywords = listOf("apollo", "hospital", "clinic", "pharmacy", "medical", "medicine", "lab", "diagnostic", "doctor", "gym", "fitness", "yoga")
        val educationKeywords = listOf("fees", "college", "school", "tuition", "course", "training", "exam", "udemy", "coursera")
        val rentKeywords = listOf("rent", "maintenance", "society", "housing", "flat")

        return when {
            incomeKeywords.any { t.contains(it) } -> "Income"
            foodKeywords.any { t.contains(it) } -> "Food"
            travelKeywords.any { t.contains(it) } -> "Travel"
            shoppingKeywords.any { t.contains(it) } -> "Shopping"
            billsKeywords.any { t.contains(it) } -> "Bills"
            healthKeywords.any { t.contains(it) } -> "Health"
            educationKeywords.any { t.contains(it) } -> "Education"
            rentKeywords.any { t.contains(it) } -> "Rent"
            else -> "Spent"
        }
    }
}

class MainViewModel : ViewModel() {
    val allExpenses = mutableStateListOf<Expense>()
    var selectedMonth = mutableStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var selectedYear = mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    var monthlyBudget = mutableStateOf(50000.0)
    fun addExpense(expense: Expense) { allExpenses.add(0, expense) }
    fun getFilteredExpenses(): List<Expense> {
        return allExpenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == selectedMonth.value && cal.get(Calendar.YEAR) == selectedYear.value
        }.sortedByDescending { it.timestamp }
    }
    fun nextMonth() { if (selectedMonth.value == 11) { selectedMonth.value = 0; selectedYear.value++ } else { selectedMonth.value++ } }
    fun prevMonth() { if (selectedMonth.value == 0) { selectedMonth.value = 11; selectedYear.value-- } else { selectedMonth.value-- } }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { MainScreen() } }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(), onResult = { permissions -> hasPermission = permissions.values.all { it } })
    LaunchedEffect(Unit) { ExpenseRepository.expensesFlow.collect { viewModel.addExpense(it) } }
    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp)); Text("Grant SMS access to track spends automatically.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) }) { Text("Grant Permission") }
            }
        }
    } else { AppNavigation(viewModel) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("SpendSense", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }) },
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text("Feed") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Info, null) }, label = { Text("Insights") })
                NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Vault") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentTab) {
                0 -> { MonthSelector(viewModel); TransactionScreen(viewModel) }
                1 -> AnalysisScreen(viewModel)
                2 -> VaultScreen(viewModel)
            }
        }
    }
}

@Composable
fun MonthSelector(viewModel: MainViewModel) {
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Month Selector
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showMonthDropdown = true },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = MONTHS[viewModel.selectedMonth.value],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            DropdownMenu(expanded = showMonthDropdown, onDismissRequest = { showMonthDropdown = false }) {
                MONTHS.forEachIndexed { index, name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        viewModel.selectedMonth.value = index
                        showMonthDropdown = false
                    })
                }
            }
        }

        // Year Selector
        Box(modifier = Modifier.weight(0.6f)) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showYearDropdown = true },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = viewModel.selectedYear.value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            DropdownMenu(expanded = showYearDropdown, onDismissRequest = { showYearDropdown = false }) {
                ((currentYear - 3)..(currentYear + 2)).forEach { year ->
                    DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                        viewModel.selectedYear.value = year
                        showYearDropdown = false
                    })
                }
            }
        }
    }
}

@Composable
fun TransactionScreen(viewModel: MainViewModel) {
    val filtered = viewModel.getFilteredExpenses()
    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(48.dp), tint = Color.LightGray); Text("No data found.", color = Color.Gray) }
        }
    } else { LazyColumn(modifier = Modifier.fillMaxSize()) { items(filtered) { TransactionItem(it) } } }
}

@Composable
fun TransactionItem(expense: Expense) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(when(expense.category) { "Income" -> Color(0xFFE8F5E9); "Refund" -> Color(0xFFE3F2FD); else -> Color(0xFFFFEBEE) }), contentAlignment = Alignment.Center) {
                    val icon = getCategoryIcon(expense.category)
                    Icon(icon, null, tint = when(expense.category) { "Income" -> Color(0xFF2E7D32); "Refund" -> Color(0xFF1565C0); else -> Color(0xFFC62828) }, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(expense.category, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (expense.isGoodSpend != null || (expense.category != "Income" && expense.category != "Refund" && expense.suggestion != null)) {
                            val isGood = expense.isGoodSpend ?: true 
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(color = if (isGood) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), shape = CircleShape) { Text(if (isGood) "Good" else "Bad", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)) }
                        }
                    }
                    Text(expense.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
                Text("₹${String.format(Locale.getDefault(), "%.0f", expense.amount)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            if (expense.suggestion != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(0.4f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp)); Text(expense.suggestion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when(category) { 
        "Food" -> Icons.Default.ShoppingCart 
        "Travel" -> Icons.Default.LocationOn 
        "Income", "Refund" -> Icons.Default.Add 
        "Bills" -> Icons.Default.Email
        "Shopping" -> Icons.Default.Face
        "Health" -> Icons.Default.Favorite
        "Education" -> Icons.Default.Star
        "Rent" -> Icons.Default.Home
        else -> Icons.Default.AccountBox 
    }
}

@Composable
fun AnalysisScreen(viewModel: MainViewModel) {
    val filtered = viewModel.getFilteredExpenses()
    val totalIncome = filtered.filter { it.category == "Income" || it.category == "Refund" }.sumOf { it.amount }
    val totalSpent = filtered.filter { it.category != "Income" && it.category != "Refund" }.sumOf { it.amount }
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard("Outflow", totalSpent, selectedTab == 0, Color(0xFFF44336), "📉", Modifier.weight(1f)) { selectedTab = 0 }
            MetricCard("Inflow", totalIncome, selectedTab == 1, Color(0xFF4CAF50), "📈", Modifier.weight(1f)) { selectedTab = 1 }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column {
                Text(if (selectedTab == 0) "Spending Pulse" else "Income Flow", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Monthly data visualization", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CumulativeDataChart(filtered, viewModel.selectedMonth.value, viewModel.selectedYear.value, if (selectedTab == 0) viewModel.monthlyBudget.value else 0.0, selectedTab == 0, selectedTab == 1)
        if (selectedTab == 0) {
            Spacer(modifier = Modifier.height(24.dp))
            BudgetEditor(viewModel)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MetricCard(label: String, amount: Double, isSelected: Boolean, color: Color, emoji: String, modifier: Modifier, onClick: () -> Unit) {
    val transition = updateTransition(isSelected, label = "card")
    val elevation by transition.animateDp(label = "elev") { if (it) 12.dp else 2.dp }
    val cardScale by transition.animateFloat(label = "scale") { if (it) 1.05f else 1f }
    
    Surface(
        onClick = onClick,
        modifier = modifier.scale(cardScale),
        shape = RoundedCornerShape(28.dp),
        color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        shadowElevation = elevation
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.weight(1f))
                Text(emoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("₹${formatAmount(amount)}", color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun BudgetEditor(viewModel: MainViewModel) {
    Surface(modifier = Modifier.padding(horizontal = 24.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💎", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Monthly Limit", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${formatAmount(viewModel.monthlyBudget.value)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("💰", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Slider(value = viewModel.monthlyBudget.value.toFloat(), onValueChange = { viewModel.monthlyBudget.value = it.toDouble() }, valueRange = 0f..200000f)
        }
    }
}

@Composable
fun CumulativeDataChart(expenses: List<Expense>, month: Int, year: Int, target: Double, showTarget: Boolean, isIncome: Boolean) {
    val cal = Calendar.getInstance(); cal.set(year, month, 1); val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val daily = DoubleArray(days + 1); val filtered = if (isIncome) expenses.filter { it.category == "Income" || it.category == "Refund" } else expenses.filter { it.category != "Income" && it.category != "Refund" }
    filtered.forEach { e -> cal.timeInMillis = e.timestamp; val d = cal.get(Calendar.DAY_OF_MONTH); if (d <= days) daily[d] += e.amount }
    val cumulative = DoubleArray(days + 1); var sum = 0.0; for (i in 1..days) { sum += daily[i]; cumulative[i] = sum }
    val max = (cumulative.maxOrNull() ?: 0.0).coerceAtLeast(target).coerceAtLeast(1000.0)
    val chartColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val spX = w / (if (days > 1) days - 1 else 1)
            for (i in 0..4) { val y = h - (i * h / 4); drawLine(Color.LightGray.copy(0.2f), Offset(0f, y), Offset(w, y), 1f) }
            if (showTarget) { val tY = h - ((target / max) * h).toFloat(); drawLine(Color.Blue.copy(0.3f), Offset(0f, tY), Offset(w, tY), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))) }
            val path = Path().apply { moveTo(0f, h); for (i in 1..days) { lineTo((i - 1) * spX, h - ((cumulative[i] / max) * h).toFloat()) }; lineTo(w, h); close() }
            drawPath(path, Brush.verticalGradient(listOf(chartColor.copy(0.3f), Color.Transparent)))
            val line = Path().apply { moveTo(0f, h - ((cumulative[1] / max) * h).toFloat()); for (i in 2..days) lineTo((i - 1) * spX, h - ((cumulative[i] / max) * h).toFloat()) }
            drawPath(line, chartColor, style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun VaultScreen(viewModel: MainViewModel) {
    val filtered = viewModel.getFilteredExpenses()
    val categories = filtered.groupBy { it.category }.map { (cat, list) -> cat to list.sumOf { it.amount } }.sortedByDescending { it.second }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedCategory == null) {
            Text("Vault", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(24.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(categories) { (cat, total) ->
                    CategoryCard(cat, total) { selectedCategory = cat }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedCategory = null }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                Text(selectedCategory!!, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            }
            val catExpenses = filtered.filter { it.category == selectedCategory }
            LazyColumn(modifier = Modifier.fillMaxSize()) { items(catExpenses) { TransactionItem(it) } }
        }
    }
}

@Composable
fun CategoryCard(category: String, amount: Double, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(getCategoryIcon(category), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("₹${formatAmount(amount)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

fun formatAmount(a: Double): String = if (a >= 1000) String.format(Locale.getDefault(), "%.1fK", a / 1000.0) else String.format(Locale.getDefault(), "%.0f", a)
