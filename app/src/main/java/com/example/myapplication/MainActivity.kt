package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val category: String
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

    fun categorize(text: String): String {
        val t = text.lowercase()
        val incomeKeywords = listOf("credited", "received", "deposit", "refund", "cashback", "salary", "added", "reversal")
        val foodKeywords = listOf("swiggy", "zomato", "eatclub", "dominos", "pizza", "kfc", "mcdonald", "restaurant", "food", "cafe", "hotel")
        val travelKeywords = listOf("uber", "ola", "rapido", "metro", "bus", "irctc", "railway", "flight", "airlines", "fuel", "petrol", "diesel", "fastag", "toll")
        val shoppingKeywords = listOf("amazon", "flipkart", "myntra", "meesho", "ajio", "snapdeal", "store", "mart", "supermarket", "reliance", "dmart")
        val billsKeywords = listOf("electricity", "eb", "water", "gas", "broadband", "wifi", "bill", "postpaid", "prepaid", "recharge", "airtel", "jio", "bsnl", "vi")
        val healthKeywords = listOf("apollo", "hospital", "clinic", "pharmacy", "medical", "medicine", "lab", "diagnostic")
        val educationKeywords = listOf("fees", "college", "school", "tuition", "course", "training", "exam")
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

    fun addExpense(expense: Expense) {
        allExpenses.add(0, expense)
    }

    fun getFilteredExpenses(): List<Expense> {
        return allExpenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == selectedMonth.value && cal.get(Calendar.YEAR) == selectedYear.value
        }.sortedByDescending { it.timestamp }
    }

    fun nextMonth() {
        if (selectedMonth.value == 11) {
            selectedMonth.value = 0
            selectedYear.value++
        } else {
            selectedMonth.value++
        }
    }

    fun prevMonth() {
        if (selectedMonth.value == 0) {
            selectedMonth.value = 11
            selectedYear.value--
        } else {
            selectedMonth.value--
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasPermission = permissions.values.all { it }
        }
    )

    LaunchedEffect(Unit) {
        ExpenseRepository.expensesFlow.collect { expense ->
            viewModel.addExpense(expense)
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Local Expense Tracker needs SMS access to track your spends automatically.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)) }) {
                    Text("Grant Permission")
                }
            }
        }
    } else {
        AppNavigation(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Local Expense Tracker", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Transactions") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Analysis") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (currentTab == 0) {
                MonthSelector(viewModel)
                TransactionScreen(viewModel)
            } else {
                AnalysisScreen(viewModel)
            }
        }
    }
}

@Composable
fun MonthSelector(viewModel: MainViewModel) {
    var showDropdown by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { showDropdown = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${MONTHS[viewModel.selectedMonth.value].take(3)} ${viewModel.selectedYear.value}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
            MONTHS.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        viewModel.selectedMonth.value = index
                        showDropdown = false
                    }
                )
            }
        }
    }
}

@Composable
fun TransactionScreen(viewModel: MainViewModel) {
    val filtered = viewModel.getFilteredExpenses()
    if (filtered.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                Text("No transactions for this month.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered) { expense ->
                TransactionItem(expense)
            }
        }
    }
}

@Composable
fun TransactionItem(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(
                    if (expense.category == "Income") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when(expense.category) {
                    "Food" -> Icons.Default.ShoppingCart
                    "Travel" -> Icons.Default.LocationOn
                    "Income" -> Icons.Default.Add
                    "Shopping" -> Icons.Default.Search
                    "Bills" -> Icons.Default.Email
                    else -> Icons.Default.AccountBox
                }
                Icon(icon, contentDescription = null, tint = if (expense.category == "Income") Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(expense.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(expense.dateString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = "Rs ${String.format(Locale.getDefault(), "%.2f", expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (expense.category == "Income") Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
fun AnalysisScreen(viewModel: MainViewModel) {
    val filtered = viewModel.getFilteredExpenses()
    val totalIncome = filtered.filter { it.category == "Income" }.sumOf { it.amount }
    val totalSpent = filtered.filter { it.category != "Income" }.sumOf { it.amount }
    
    var selectedSummary by remember { mutableStateOf("Spent") }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        AnalysisTopTabs()
        AnalysisFilterRow()
        Spacer(modifier = Modifier.height(16.dp))
        AnalysisMonthNavigator(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            SummaryBox("Spent", totalSpent, selectedSummary == "Spent", Modifier.weight(1f)) {
                selectedSummary = "Spent"
            }
            SummaryBox("Incoming", totalIncome, selectedSummary == "Incoming", Modifier.weight(1f)) {
                selectedSummary = "Incoming"
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        if (selectedSummary == "Spent") {
            BudgetInfoRow(totalSpent, viewModel)
        } else {
            IncomeInfoRow(totalIncome)
        }
        Spacer(modifier = Modifier.height(16.dp))
        CumulativeDataChart(
            expenses = filtered, 
            month = viewModel.selectedMonth.value, 
            year = viewModel.selectedYear.value, 
            targetValue = if (selectedSummary == "Spent") viewModel.monthlyBudget.value else 0.0,
            showTarget = selectedSummary == "Spent",
            isIncome = selectedSummary == "Incoming"
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AnalysisTopTabs() {
    val tabs = listOf("Bank Accounts", "Cards", "Net Worth")
    var selectedTab by remember { mutableStateOf(0) }
    
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 16.dp,
        containerColor = Color.White,
        contentColor = Color.Black,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color.Black
                )
            }
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisFilterRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Filter by", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = true,
            onClick = {},
            label = { Text("All") },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = CircleShape
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = false,
            onClick = {},
            label = { Text("Federal Bank • 3196") },
            leadingIcon = { 
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.Red), 
                    contentAlignment = Alignment.Center
                ) { 
                    Text("F", color = Color.White, fontSize = 10.sp) 
                } 
            },
            shape = CircleShape
        )
    }
}

@Composable
fun AnalysisMonthNavigator(viewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.prevMonth() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
        }
        Text(
            text = "${MONTHS[viewModel.selectedMonth.value]} ${viewModel.selectedYear.value}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = { viewModel.nextMonth() }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
        }
    }
}

@Composable
fun SummaryBox(label: String, amount: Double, isActive: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isActive) Color.Black else Color.White
    val contentColor = if (isActive) Color.White else Color.Black
    val borderColor = Color.LightGray.copy(alpha = 0.5f)

    Surface(
        modifier = modifier.fillMaxHeight().clickable { onClick() },
        color = backgroundColor,
        border = if (!isActive) BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
            Text(
                text = "₹${formatAmount(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}

fun formatAmount(amount: Double): String {
    return if (amount >= 1000) {
        String.format(Locale.getDefault(), "%.1fK", amount / 1000.0)
    } else {
        String.format(Locale.getDefault(), "%.0f", amount)
    }
}

@Composable
fun BudgetInfoRow(spent: Double, viewModel: MainViewModel) {
    var showSlider by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("This month", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Last month", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatAmount(spent)}/${formatAmount(viewModel.monthlyBudget.value)}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Budget", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary, 
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    modifier = Modifier.clickable { showSlider = !showSlider }
                )
            }
            Text("0", fontWeight = FontWeight.Bold)
        }
        
        if (showSlider) {
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = viewModel.monthlyBudget.value.toFloat(),
                onValueChange = { viewModel.monthlyBudget.value = it.toDouble() },
                valueRange = 0f..200000f,
                steps = 19
            )
            Text(
                text = "Set Budget: ₹${formatAmount(viewModel.monthlyBudget.value)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun IncomeInfoRow(income: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("This month income", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Last month income", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(formatAmount(income), fontWeight = FontWeight.Bold)
            Text("0", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CumulativeDataChart(
    expenses: List<Expense>, 
    month: Int, 
    year: Int, 
    targetValue: Double,
    showTarget: Boolean,
    isIncome: Boolean
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val dailyData = DoubleArray(daysInMonth + 1) { 0.0 }
    val filteredExpenses = if (isIncome) {
        expenses.filter { it.category == "Income" }
    } else {
        expenses.filter { it.category != "Income" }
    }
    
    for (expense in filteredExpenses) {
        calendar.timeInMillis = expense.timestamp
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        if (day <= daysInMonth) {
            dailyData[day] += expense.amount
        }
    }
    
    val cumulativeData = DoubleArray(daysInMonth + 1) { 0.0 }
    var sum = 0.0
    for (i in 1..daysInMonth) {
        sum += dailyData[i]
        cumulativeData[i] = sum
    }
    
    val maxChartVal = (cumulativeData.maxOrNull() ?: 0.0).coerceAtLeast(targetValue).coerceAtLeast(1000.0)
    val chartColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFF2E7D32) // Keeping green as per reference
    val areaColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFF4CAF50)

    Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacingX = width / (if (daysInMonth > 1) (daysInMonth - 1) else 1).toFloat()
            
            // Draw grid lines
            val gridLines = 5
            for (i in 0..gridLines) {
                val y = height - (i * height / gridLines)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Target dashed line (e.g. Budget)
            if (showTarget) {
                val targetY = height - ((targetValue / maxChartVal) * height.toDouble()).toFloat()
                if (targetY in 0f..height) {
                    drawLine(
                        color = Color.Blue.copy(alpha = 0.5f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }

            // Area path
            val path = Path()
            path.moveTo(0f, height)
            for (i in 1..daysInMonth) {
                val x = (i - 1) * spacingX
                val y = height - ((cumulativeData[i] / maxChartVal) * height.toDouble()).toFloat()
                path.lineTo(x, y)
            }
            path.lineTo(width, height)
            path.close()
            drawPath(path, brush = Brush.verticalGradient(listOf(areaColor.copy(alpha = 0.3f), Color.Transparent)))

            // Line path
            val linePath = Path()
            if (daysInMonth > 0) {
                linePath.moveTo(0f, height - ((cumulativeData[1] / maxChartVal) * height.toDouble()).toFloat())
                for (i in 2..daysInMonth) {
                    val x = (i - 1) * spacingX
                    val y = height - ((cumulativeData[i] / maxChartVal) * height.toDouble()).toFloat()
                    linePath.lineTo(x, y)
                }
                drawPath(linePath, color = chartColor, style = Stroke(width = 4f))
                
                // Last point dot
                val lastX = (daysInMonth - 1) * spacingX
                val lastY = height - ((cumulativeData[daysInMonth] / maxChartVal) * height.toDouble()).toFloat()
                drawCircle(color = chartColor, radius = 8f, center = Offset(lastX, lastY))
            }
        }
        
        // Y-axis labels (Dynamic)
        Column(modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd), verticalArrangement = Arrangement.SpaceBetween) {
            Text("₹${formatAmount(maxChartVal)}", fontSize = 10.sp, color = Color.Gray)
            Text("₹${formatAmount(maxChartVal*0.8)}", fontSize = 10.sp, color = Color.Gray)
            Text("₹${formatAmount(maxChartVal*0.6)}", fontSize = 10.sp, color = Color.Gray)
            Text("₹${formatAmount(maxChartVal*0.4)}", fontSize = 10.sp, color = Color.Gray)
            Text("₹${formatAmount(maxChartVal*0.2)}", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(1.dp))
        }
        
        // X-axis labels
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1 ${MONTHS[month].take(3)}", fontSize = 10.sp, color = Color.Gray)
            Text("15 ${MONTHS[month].take(3)}", fontSize = 10.sp, color = Color.Gray)
            Text("${daysInMonth} ${MONTHS[month].take(3)}", fontSize = 10.sp, color = Color.Gray)
        }
    }
}
