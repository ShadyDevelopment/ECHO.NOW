package com.echonow.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import com.echonow.presentation.components.EchoButton as Button
import com.echonow.presentation.components.EchoOutlinedButton as OutlinedButton
import com.echonow.presentation.components.EchoTextButton as TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echonow.data.remote.SubtitlesApi
import com.echonow.domain.repository.UserPreferences
import com.echonow.platform.configuredAppName
import com.echonow.platform.configuredAppVersion
import com.echonow.platform.configuredAppWeb
import com.echonow.platform.configuredSubdlApiKey
import com.echonow.platform.PlatformDeviceInfo
import com.echonow.platform.readPlatformDeviceInfo
import com.echonow.platform.DialogBackdropEffect
import com.echonow.presentation.components.SectionTitle
import com.echonow.theme.LocalButtonCustomization
import com.echonow.presentation.components.DevicePairingDialog
import com.echonow.domain.repository.PairedDevice
import com.echonow.platform.observePairedDevices
import com.echonow.platform.setDevicePairingMode
import com.echonow.platform.unpairDevice
import com.echonow.platform.updateLocalDeviceName
import com.echonow.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private data class Country(val name: String, val code: String, val subtitleLanguage: String)

private fun Color.asPreferenceColor(): Long = toArgb().toLong() and 0xFFFFFFFFL

private val countries = listOf(
    Country("Afghanistan", "AF", "PS"), Country("Albania", "AL", "SQ"), Country("Algeria", "DZ", "AR"),
    Country("Andorra", "AD", "CA"), Country("Angola", "AO", "PT"), Country("Antigua and Barbuda", "AG", "EN"),
    Country("Argentina", "AR", "ES"), Country("Armenia", "AM", "HY"), Country("Australia", "AU", "EN"),
    Country("Austria", "AT", "DE"), Country("Azerbaijan", "AZ", "AZ"), Country("Bahamas", "BS", "EN"),
    Country("Bahrain", "BH", "AR"), Country("Bangladesh", "BD", "BN"), Country("Barbados", "BB", "EN"),
    Country("Belarus", "BY", "BE"), Country("Belgium", "BE", "NL"), Country("Belize", "BZ", "EN"),
    Country("Benin", "BJ", "FR"), Country("Bhutan", "BT", "DZ"), Country("Bolivia", "BO", "ES"),
    Country("Bosnia and Herzegovina", "BA", "BS"), Country("Botswana", "BW", "EN"), Country("Brazil", "BR", "PT"),
    Country("Brunei", "BN", "MS"), Country("Bulgaria", "BG", "BG"), Country("Burkina Faso", "BF", "FR"),
    Country("Burundi", "BI", "RN"), Country("Cabo Verde", "CV", "PT"), Country("Cambodia", "KH", "KM"),
    Country("Cameroon", "CM", "FR"), Country("Canada", "CA", "EN"), Country("Central African Republic", "CF", "FR"),
    Country("Chad", "TD", "FR"), Country("Chile", "CL", "ES"), Country("China", "CN", "ZH"),
    Country("Colombia", "CO", "ES"), Country("Comoros", "KM", "AR"), Country("Congo", "CG", "FR"),
    Country("Costa Rica", "CR", "ES"), Country("Côte d’Ivoire", "CI", "FR"), Country("Croatia", "HR", "HR"),
    Country("Cuba", "CU", "ES"), Country("Cyprus", "CY", "EL"), Country("Czechia", "CZ", "CS"),
    Country("Democratic Republic of the Congo", "CD", "FR"), Country("Denmark", "DK", "DA"), Country("Djibouti", "DJ", "FR"),
    Country("Dominica", "DM", "EN"), Country("Dominican Republic", "DO", "ES"), Country("Ecuador", "EC", "ES"),
    Country("Egypt", "EG", "AR"), Country("El Salvador", "SV", "ES"), Country("Equatorial Guinea", "GQ", "ES"),
    Country("Eritrea", "ER", "TI"), Country("Estonia", "EE", "ET"), Country("Eswatini", "SZ", "EN"),
    Country("Ethiopia", "ET", "AM"), Country("Fiji", "FJ", "EN"), Country("Finland", "FI", "FI"),
    Country("France", "FR", "FR"), Country("Gabon", "GA", "FR"), Country("Gambia", "GM", "EN"),
    Country("Georgia", "GE", "KA"), Country("Germany", "DE", "DE"), Country("Ghana", "GH", "EN"),
    Country("Greece", "GR", "EL"), Country("Grenada", "GD", "EN"), Country("Guatemala", "GT", "ES"),
    Country("Guinea", "GN", "FR"), Country("Guinea-Bissau", "GW", "PT"), Country("Guyana", "GY", "EN"),
    Country("Haiti", "HT", "HT"), Country("Honduras", "HN", "ES"), Country("Hungary", "HU", "HU"),
    Country("Iceland", "IS", "IS"), Country("India", "IN", "HI"), Country("Indonesia", "ID", "ID"),
    Country("Iran", "IR", "FA"), Country("Iraq", "IQ", "AR"), Country("Ireland", "IE", "EN"),
    Country("Israel", "IL", "HE"), Country("Italy", "IT", "IT"), Country("Jamaica", "JM", "EN"),
    Country("Japan", "JP", "JA"), Country("Jordan", "JO", "AR"), Country("Kazakhstan", "KZ", "KK"),
    Country("Kenya", "KE", "SW"), Country("Kiribati", "KI", "EN"), Country("Kuwait", "KW", "AR"),
    Country("Kyrgyzstan", "KG", "KY"), Country("Laos", "LA", "LO"), Country("Latvia", "LV", "LV"),
    Country("Lebanon", "LB", "AR"), Country("Lesotho", "LS", "EN"), Country("Liberia", "LR", "EN"),
    Country("Libya", "LY", "AR"), Country("Liechtenstein", "LI", "DE"), Country("Lithuania", "LT", "LT"),
    Country("Luxembourg", "LU", "FR"), Country("Madagascar", "MG", "MG"), Country("Malawi", "MW", "EN"),
    Country("Malaysia", "MY", "MS"), Country("Maldives", "MV", "DV"), Country("Mali", "ML", "FR"),
    Country("Malta", "MT", "MT"), Country("Marshall Islands", "MH", "EN"), Country("Mauritania", "MR", "AR"),
    Country("Mauritius", "MU", "EN"), Country("Mexico", "MX", "ES"), Country("Micronesia", "FM", "EN"),
    Country("Moldova", "MD", "RO"), Country("Monaco", "MC", "FR"), Country("Mongolia", "MN", "MN"),
    Country("Montenegro", "ME", "SR"), Country("Morocco", "MA", "AR"), Country("Mozambique", "MZ", "PT"),
    Country("Myanmar", "MM", "MY"), Country("Namibia", "NA", "EN"), Country("Nauru", "NR", "EN"),
    Country("Nepal", "NP", "NE"), Country("Netherlands", "NL", "NL"), Country("New Zealand", "NZ", "EN"),
    Country("Nicaragua", "NI", "ES"), Country("Niger", "NE", "FR"), Country("Nigeria", "NG", "EN"),
    Country("North Korea", "KP", "KO"), Country("North Macedonia", "MK", "MK"), Country("Norway", "NO", "NO"),
    Country("Oman", "OM", "AR"), Country("Pakistan", "PK", "UR"), Country("Palau", "PW", "EN"),
    Country("Palestine", "PS", "AR"), Country("Panama", "PA", "ES"), Country("Papua New Guinea", "PG", "EN"),
    Country("Paraguay", "PY", "ES"), Country("Peru", "PE", "ES"), Country("Philippines", "PH", "TL"),
    Country("Poland", "PL", "PL"), Country("Portugal", "PT", "PT"), Country("Qatar", "QA", "AR"),
    Country("Romania", "RO", "RO"), Country("Russia", "RU", "RU"), Country("Rwanda", "RW", "RW"),
    Country("Saint Kitts and Nevis", "KN", "EN"), Country("Saint Lucia", "LC", "EN"),
    Country("Saint Vincent and the Grenadines", "VC", "EN"), Country("Samoa", "WS", "SM"),
    Country("San Marino", "SM", "IT"), Country("São Tomé and Príncipe", "ST", "PT"),
    Country("Saudi Arabia", "SA", "AR"), Country("Senegal", "SN", "FR"), Country("Serbia", "RS", "SR"),
    Country("Seychelles", "SC", "FR"), Country("Sierra Leone", "SL", "EN"), Country("Singapore", "SG", "ZH"),
    Country("Slovakia", "SK", "SK"), Country("Slovenia", "SI", "SL"), Country("Solomon Islands", "SB", "EN"),
    Country("Somalia", "SO", "SO"), Country("South Africa", "ZA", "EN"), Country("South Korea", "KR", "KO"),
    Country("South Sudan", "SS", "EN"), Country("Spain", "ES", "ES"), Country("Sri Lanka", "LK", "SI"),
    Country("Sudan", "SD", "AR"), Country("Suriname", "SR", "NL"), Country("Sweden", "SE", "SV"),
    Country("Switzerland", "CH", "DE"), Country("Syria", "SY", "AR"), Country("Taiwan", "TW", "ZH"),
    Country("Tajikistan", "TJ", "TG"), Country("Tanzania", "TZ", "SW"), Country("Thailand", "TH", "TH"),
    Country("Timor-Leste", "TL", "PT"), Country("Togo", "TG", "FR"), Country("Tonga", "TO", "TO"),
    Country("Trinidad and Tobago", "TT", "EN"), Country("Tunisia", "TN", "AR"), Country("Türkiye", "TR", "TR"),
    Country("Turkmenistan", "TM", "TK"), Country("Tuvalu", "TV", "EN"), Country("Uganda", "UG", "EN"),
    Country("Ukraine", "UA", "UK"), Country("United Arab Emirates", "AE", "AR"), Country("United Kingdom", "GB", "EN"),
    Country("United States", "US", "EN"), Country("Uruguay", "UY", "ES"), Country("Uzbekistan", "UZ", "UZ"),
    Country("Vanuatu", "VU", "BI"), Country("Vatican City", "VA", "IT"), Country("Venezuela", "VE", "ES"),
    Country("Vietnam", "VN", "VI"), Country("Yemen", "YE", "AR"), Country("Zambia", "ZM", "EN"),
    Country("Zimbabwe", "ZW", "EN"),
)

@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = koinViewModel()
    val preferences by viewModel.state.collectAsState()
    val buttonCustomization = LocalButtonCustomization.current
    val toggleColors = SwitchDefaults.colors(
        checkedThumbColor = buttonCustomization.textColor ?: MaterialTheme.colorScheme.onPrimary,
        checkedTrackColor = buttonCustomization.fillColor ?: MaterialTheme.colorScheme.primary,
        checkedBorderColor = buttonCustomization.borderColor ?: MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        uncheckedBorderColor = buttonCustomization.borderColor ?: MaterialTheme.colorScheme.outline,
    )
    var dialog by remember { mutableStateOf<String?>(null) }
    var colorPicker by remember { mutableStateOf<String?>(null) }
    var expandedCategory by remember { mutableStateOf<String?>("Appearance") }
    var pairingCode by remember { mutableStateOf<String?>(null) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var deviceToUnpair by remember { mutableStateOf<PairedDevice?>(null) }
    val pairedDevices by observePairedDevices().collectAsState()
    var openSubtitlesStatus by remember { mutableStateOf<String?>(null) }
    var subdlStatus by remember { mutableStateOf<String?>(null) }
    var echoSubsStatus by remember { mutableStateOf<String?>(null) }
    var countryMenu by remember { mutableStateOf(false) }
    var deviceInfo by remember { mutableStateOf(readPlatformDeviceInfo()) }
    val subtitlesApi: SubtitlesApi = koinInject()
    val scope = rememberCoroutineScope()

    LaunchedEffect(expandedCategory) {
        if (expandedCategory == "Device") {
            deviceInfo = readPlatformDeviceInfo()
            pairingCode = (0..5).joinToString("") { (0..9).random().toString() }
            setDevicePairingMode(pairingCode, preferences.deviceName)
        } else {
            pairingCode = null
            setDevicePairingMode(null, preferences.deviceName)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { setDevicePairingMode(null, preferences.deviceName) }
    }

    val settingCategories = listOf(
        "Appearance",
        "User interface",
        "Device",
        "Date, time & language",
        "Subtitles",
        "Subtitle providers",
        "About and updates",
    )
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Settings", "Make ${configuredAppName()} feel like yours")
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val navigation: @Composable (Modifier) -> Unit = { modifier ->
                SettingsCategoryNavigation(
                    categories = settingCategories,
                    selectedCategory = expandedCategory,
                    onSelect = { expandedCategory = it },
                    modifier = modifier,
                    horizontal = maxWidth < 760.dp,
                )
            }
            val settingsContent: @Composable () -> Unit = {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
            CollapsibleSettingCategory("Appearance", expandedCategory == "Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Light", "Dark", "System").forEach { theme ->
                        if (preferences.theme == theme) Button(onClick = { viewModel.update(preferences.copy(theme = theme)) }) { Text(theme) }
                        else OutlinedButton(onClick = { viewModel.update(preferences.copy(theme = theme)) }) { Text(theme) }
                    }
                }
                Text("Color palette", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(
                        Triple(0xFF8A5CF6L, 0xFFFF4C8BL, 0xFF31A7A0L),
                        Triple(0xFF31A7A0L, 0xFFFFB547L, 0xFF4478E8L),
                        Triple(0xFF4478E8L, 0xFFF05D9CL, 0xFFCA5C28L),
                        Triple(0xFFCA5C28L, 0xFF579BFC, 0xFF31A7A0L),
                    ).forEach { (primary, secondary, tertiary) ->
                        OutlinedButton(
                            onClick = {
                                viewModel.update(
                                    preferences.copy(
                                        primaryColor = primary,
                                        secondaryColor = secondary,
                                        tertiaryColor = tertiary,
                                    ),
                                )
                            },
                        ) {
                            Text("●", color = Color(primary))
                            Text("  ●", color = Color(secondary))
                            Text("  ●", color = Color(tertiary))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { colorPicker = "primary" }) {
                        Text("Primary color", color = Color(preferences.primaryColor))
                    }
                    OutlinedButton(onClick = { colorPicker = "secondary" }) {
                        Text("Secondary color", color = Color(preferences.secondaryColor))
                    }
                    OutlinedButton(onClick = { colorPicker = "tertiary" }) {
                        Text("Third color", color = Color(preferences.tertiaryColor))
                    }
                }
                Text("Global button customization", style = MaterialTheme.typography.titleMedium)
                Text("Button shape")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Rounded", "Pill", "Square").forEach { shape ->
                        if (preferences.buttonShape == shape) {
                            Button(onClick = { viewModel.update(preferences.copy(buttonShape = shape)) }) {
                                Text(shape)
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.update(preferences.copy(buttonShape = shape)) }) {
                                Text(shape)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = { colorPicker = "button-fill" }) {
                        Text("Button fill", color = Color(preferences.buttonFillColor ?: preferences.primaryColor))
                    }
                    OutlinedButton(onClick = { colorPicker = "button-text" }) {
                        Text(
                            "Button text",
                            color = Color(preferences.buttonTextColor ?: MaterialTheme.colorScheme.onPrimary.asPreferenceColor()),
                        )
                    }
                    OutlinedButton(onClick = { colorPicker = "button-border" }) {
                        Text(
                            "Button border",
                            color = Color(preferences.buttonBorderColor ?: MaterialTheme.colorScheme.outline.asPreferenceColor()),
                        )
                    }
                }
                if (
                    preferences.buttonFillColor != null ||
                    preferences.buttonTextColor != null ||
                    preferences.buttonBorderColor != null
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.update(
                                preferences.copy(
                                    buttonFillColor = null,
                                    buttonTextColor = null,
                                    buttonBorderColor = null,
                                ),
                            )
                        },
                    ) {
                        Text("Use theme colors for buttons")
                    }
                }
            }

            CollapsibleSettingCategory("User interface", expandedCategory == "User interface") {
                Row(
                    modifier = Modifier.fillMaxWidth(.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show menu item labels")
                    Switch(
                        checked = preferences.showMenuLabels,
                        onCheckedChange = { viewModel.update(preferences.copy(showMenuLabels = it)) },
                        colors = toggleColors,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Collapse side navigation")
                        Text(
                            "Use a compact icon-only sidebar on wide screens",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = preferences.navigationBarCollapsed,
                        onCheckedChange = {
                            viewModel.update(preferences.copy(navigationBarCollapsed = it))
                        },
                        colors = toggleColors,
                    )
                }
                Text("Interface layout")
                var interfaceMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { interfaceMenu = true }) {
                        Text(
                            when (preferences.interfaceMode) {
                                "PHONE" -> "Phone"
                                "TV" -> "TV"
                                else -> "Default (detected device)"
                            },
                        )
                    }
                    DropdownMenu(expanded = interfaceMenu, onDismissRequest = { interfaceMenu = false }) {
                        listOf("AUTO" to "Default (detected device)", "PHONE" to "Phone", "TV" to "TV").forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.update(preferences.copy(interfaceMode = mode))
                                    interfaceMenu = false
                                },
                            )
                        }
                    }
                }
                Text(
                    "Default follows the device and current orientation. Phone uses bottom navigation; TV uses a side rail.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CollapsibleSettingCategory("Device", expandedCategory == "Device") {
                Text("Device name")
                OutlinedTextField(
                    value = preferences.deviceName,
                    onValueChange = { name ->
                        viewModel.update(preferences.copy(deviceName = name.take(40)))
                        updateLocalDeviceName(name)
                    },
                    label = { Text("Name shown to paired devices") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Text("Device info", style = MaterialTheme.typography.titleMedium)
                Column(
                    Modifier.fillMaxWidth(.72f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DeviceInfoRow("Device model", deviceInfo.model)
                    DeviceInfoRow("Operating system", deviceInfo.operatingSystem)
                    DeviceInfoRow("Architecture", deviceInfo.architecture)
                    DeviceInfoRow("Network", deviceInfo.network)
                    DeviceInfoRow("Device IP", deviceInfo.ipAddresses)
                    DeviceInfoRow("MAC address", deviceInfo.macAddresses)
                    OutlinedButton(onClick = { deviceInfo = readPlatformDeviceInfo() }) {
                        Text("Refresh device info")
                    }
                }
                Text("Pairing", style = MaterialTheme.typography.titleMedium)
                Text(
                    "One-time pairing code: ${pairingCode ?: "Open this section to generate a code"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "This code is active only while Settings > Device is open. Keep this section open on both devices while pairing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                OutlinedButton(onClick = { showPairingDialog = true }) { Text("Pair with another device") }
                Text("Paired devices", style = MaterialTheme.typography.titleMedium)
                if (pairedDevices.isEmpty()) {
                    Text("You don't have any paired devices!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        Modifier.fillMaxWidth(.72f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        pairedDevices.forEach { device ->
                            OutlinedButton(
                                onClick = { deviceToUnpair = device },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${device.name}  ·  Unpair")
                            }
                        }
                    }
                }
            }

            CollapsibleSettingCategory("Date, time & language", expandedCategory == "Date, time & language") {
                Text("Clock source", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("AUTO" to "Automatic", "MANUAL" to "Manual").forEach { (mode, label) ->
                        if (preferences.clockMode == mode) {
                            Button(onClick = { viewModel.update(preferences.copy(clockMode = mode)) }) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { viewModel.update(preferences.copy(clockMode = mode)) }) { Text(label) }
                        }
                    }
                }
                Text(
                    "Manual date/time changes ECHO NOW's clock display only; changing the device clock requires system-level permission.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                if (preferences.clockMode == "MANUAL") {
                    OutlinedTextField(
                        value = preferences.manualClockValue,
                        onValueChange = {
                            viewModel.update(preferences.copy(manualClockValue = it.take(16)))
                        },
                        label = { Text("Date and time") },
                        placeholder = { Text("2026-10-17 15:30") },
                        supportingText = { Text("Use yyyy-MM-dd HH:mm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(.72f),
                    )
                }
                var timeZoneMenu by remember { mutableStateOf(false) }
                val timeZones = listOf(
                    "AUTO" to "Automatic (device timezone)",
                    "UTC" to "UTC",
                    "America/New_York" to "America/New_York",
                    "America/Los_Angeles" to "America/Los_Angeles",
                    "Europe/London" to "Europe/London",
                    "Europe/Paris" to "Europe/Paris",
                    "Asia/Kolkata" to "Asia/Kolkata",
                    "Asia/Tokyo" to "Asia/Tokyo",
                    "Australia/Sydney" to "Australia/Sydney",
                )
                Box {
                    OutlinedButton(onClick = { timeZoneMenu = true }) {
                        Text("Time zone: ${timeZones.firstOrNull { it.first == preferences.timeZoneId }?.second ?: preferences.timeZoneId}")
                    }
                    DropdownMenu(expanded = timeZoneMenu, onDismissRequest = { timeZoneMenu = false }) {
                        timeZones.forEach { (zone, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.update(preferences.copy(timeZoneId = zone))
                                    timeZoneMenu = false
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("24-hour time")
                    Switch(
                        checked = preferences.timeFormat24Hour,
                        onCheckedChange = { enabled ->
                            viewModel.update(
                                preferences.copy(
                                    timeFormat24Hour = enabled,
                                    clockPattern = if (enabled) "EEE, MMM d · HH:mm" else "EEE, MMM d · h:mm a",
                                ),
                            )
                        },
                        colors = toggleColors,
                    )
                }
                Text("Date/time display", style = MaterialTheme.typography.titleMedium)
                listOf(
                    "Show on Home" to preferences.showClockOnHome,
                    "Show on other pages" to preferences.showClockOnOtherPages,
                ).forEachIndexed { index, (label, checked) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(.72f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label)
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                if (index == 0) viewModel.update(preferences.copy(showClockOnHome = enabled))
                                else viewModel.update(preferences.copy(showClockOnOtherPages = enabled))
                            },
                            colors = toggleColors,
                        )
                    }
                }
                OutlinedTextField(
                    value = preferences.clockPattern,
                    onValueChange = { viewModel.update(preferences.copy(clockPattern = it.take(64))) },
                    label = { Text("Date/time format") },
                    supportingText = { Text("Java date pattern, e.g. EEE, MMM d · h:mm a") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Text("Clock text size: ${preferences.clockSizeSp} sp")
                Slider(
                    value = preferences.clockSizeSp.toFloat(),
                    onValueChange = { viewModel.update(preferences.copy(clockSizeSp = it.toInt())) },
                    valueRange = 10f..28f,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0xFFFFFFFFL, 0xFFFFD166L, 0xFF9BE7FFL, 0xFFD8B4F8L).forEach { color ->
                        OutlinedButton(onClick = { viewModel.update(preferences.copy(clockColor = color)) }) {
                            Text("●", color = Color(color))
                        }
                    }
                }
                var languageMenu by remember { mutableStateOf(false) }
                val languages = listOf(
                    "SYSTEM" to "System language",
                    "en-US" to "English (United States)",
                    "es-ES" to "Español",
                    "fr-FR" to "Français",
                    "de-DE" to "Deutsch",
                    "ja-JP" to "日本語",
                )
                Box {
                    OutlinedButton(onClick = { languageMenu = true }) {
                        Text("Language: ${languages.firstOrNull { it.first == preferences.languageTag }?.second ?: preferences.languageTag}")
                    }
                    DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                        languages.forEach { (tag, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.update(preferences.copy(languageTag = tag))
                                    languageMenu = false
                                },
                            )
                        }
                    }
                }
                Text(
                    "This locale controls date/time formatting. The app interface remains in English.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CollapsibleSettingCategory("Subtitles", expandedCategory == "Subtitles") {
                val subtitleStyle = preferences.subtitleStyle
                Text("Preview", style = MaterialTheme.typography.titleMedium)
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().widthIn(max = 680.dp).aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF52646C), Color(0xFF28323A), Color(0xFF10141A)),
                                ),
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = .08f), Color.Transparent, Color.Black.copy(alpha = .55f)),
                                ),
                            ),
                        )
                        Text(
                            "This is a preview of your subtitle configuration",
                            modifier = Modifier.padding(
                                bottom = 18.dp + maxHeight * (preferences.subtitleVerticalPositionPercent.coerceIn(0, 25) / 100f),
                                start = 12.dp,
                                end = 12.dp,
                            ).then(
                                if (subtitleStyle.boxEnabled) {
                                    Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Color(subtitleStyle.background).copy(
                                                alpha = (100 - subtitleStyle.boxTransparency.coerceIn(0, 100)) / 100f,
                                            ),
                                        )
                                        .border(1.dp, Color(subtitleStyle.border), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                } else Modifier,
                            ),
                            color = Color(subtitleStyle.text),
                            fontSize = subtitleStyle.sizeSp.coerceIn(14, 36).sp,
                            fontFamily = subtitleFontFamily(subtitleStyle.fontFamily),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Box {
                    OutlinedButton(onClick = { countryMenu = true }) {
                        Text("Subtitle country: ${countries.firstOrNull { it.code == preferences.subtitleCountry }?.name ?: "United States"}")
                    }
                    DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country.name) },
                                onClick = {
                                    viewModel.update(
                                        preferences.copy(
                                            subtitleCountry = country.code,
                                            subtitleLanguage = country.subtitleLanguage,
                                        ),
                                    )
                                    countryMenu = false
                                },
                            )
                        }
                    }
                }
                Text(
                    "Subtitle language: ${preferences.subtitleLanguage}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Automatically apply downloaded subtitles")
                    Switch(
                        checked = preferences.autoApplySubtitles,
                        onCheckedChange = { viewModel.update(preferences.copy(autoApplySubtitles = it)) },
                        colors = toggleColors,
                    )
                }
                Text("Text size: ${subtitleStyle.sizeSp} sp")
                Slider(
                    value = subtitleStyle.sizeSp.toFloat(),
                    onValueChange = {
                        viewModel.update(preferences.copy(subtitleStyle = subtitleStyle.copy(sizeSp = it.toInt())))
                    },
                    valueRange = 14f..36f,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Text("Subtitle vertical position: ${preferences.subtitleVerticalPositionPercent.coerceIn(0, 25)}%")
                Slider(
                    value = preferences.subtitleVerticalPositionPercent.coerceIn(0, 25).toFloat(),
                    onValueChange = {
                        viewModel.update(preferences.copy(subtitleVerticalPositionPercent = it.toInt()))
                    },
                    valueRange = 0f..25f,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { colorPicker = "subtitle-text" }) { Text("Text color") }
                    OutlinedButton(
                        onClick = { colorPicker = "subtitle-background" },
                        enabled = subtitleStyle.boxEnabled,
                    ) { Text("Background color") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(.72f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Subtitle background box")
                    Switch(
                        checked = subtitleStyle.boxEnabled,
                        onCheckedChange = {
                            viewModel.update(preferences.copy(subtitleStyle = subtitleStyle.copy(boxEnabled = it)))
                        },
                        colors = toggleColors,
                    )
                }
                if (subtitleStyle.boxEnabled) {
                    Text("Box transparency: ${subtitleStyle.boxTransparency}%")
                    Slider(
                        value = subtitleStyle.boxTransparency.toFloat(),
                        onValueChange = {
                            viewModel.update(
                                preferences.copy(subtitleStyle = subtitleStyle.copy(boxTransparency = it.toInt())),
                            )
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(.72f),
                    )
                }
                var fontMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { fontMenu = true }) { Text("Font: ${subtitleStyle.fontFamily}") }
                    DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                        listOf("SansSerif", "Serif", "Monospace", "Cursive").forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font) },
                                onClick = {
                                    viewModel.update(preferences.copy(subtitleStyle = subtitleStyle.copy(fontFamily = font)))
                                    fontMenu = false
                                },
                            )
                        }
                    }
                }
            }

            CollapsibleSettingCategory("Subtitle providers", expandedCategory == "Subtitle providers") {
                Text("ECHOSubs API (Default)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Built-in OpenSubtitles engine. Directly retrieves and downloads subtitles with no server, configuration, or API key required.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Button(onClick = {
                    scope.launch {
                        echoSubsStatus = "Checking ECHOSubs API connection…"
                        echoSubsStatus = runCatching {
                            subtitlesApi.testEchoSubsConnection(preferences.subtitleLanguage)
                            "Connected to ECHOSubs API."
                        }.getOrElse { "Connection failed: ${it.message ?: "request error"}" }
                    }
                }) { Text("Test ECHOSubs API connection") }
                echoSubsStatus?.let {
                    Text(
                        it,
                        color = if (it.contains("failed", ignoreCase = true) || it.contains("error", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("OpenSubtitles (External API Key)")
                OutlinedTextField(
                    value = preferences.openSubtitlesApiKey,
                    onValueChange = {
                        openSubtitlesStatus = null
                        viewModel.update(preferences.copy(openSubtitlesApiKey = it))
                    },
                    label = { Text("OpenSubtitles API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Button(onClick = {
                    scope.launch {
                        openSubtitlesStatus = "Checking connection…"
                        openSubtitlesStatus = runCatching {
                            subtitlesApi.testOpenSubtitlesConnection(preferences.openSubtitlesApiKey)
                            "Connected to OpenSubtitles."
                        }.getOrElse { "Connection failed: ${it.message ?: "request error"}" }
                    }
                }) { Text("Test OpenSubtitles connection") }
                openSubtitlesStatus?.let {
                    Text(
                        it,
                        color = if (it.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text("SubDL")
                OutlinedTextField(
                    value = preferences.subdlApiKey,
                    onValueChange = {
                        subdlStatus = null
                        viewModel.update(preferences.copy(subdlApiKey = it))
                    },
                    label = { Text("Personal SubDL API key (optional)") },
                    supportingText = { Text("Leave blank to use the app-configured SubDL connection.") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(.72f),
                )
                Button(onClick = {
                    scope.launch {
                        subdlStatus = "Checking SubDL connection…"
                        subdlStatus = runCatching {
                            subtitlesApi.testSubdlConnection(preferences.subdlApiKey.ifBlank { configuredSubdlApiKey() })
                            "Connected to SubDL."
                        }.getOrElse { "SubDL connection failed: ${it.message ?: "request error"}" }
                    }
                }) { Text("Test SubDL connection") }
                Text(
                    subdlStatus ?: if (preferences.subdlApiKey.isNotBlank()) "Using your personal SubDL key."
                    else if (configuredSubdlApiKey().isNotBlank()) "Using the app-configured SubDL key."
                    else "No SubDL key is configured.",
                    color = if (subdlStatus?.contains("failed", ignoreCase = true) == true) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            CollapsibleSettingCategory("About and updates", expandedCategory == "About and updates") {
                Text("App: ${configuredAppName()}")
                Text("Version: ${configuredAppVersion()}")
                Text("Developer: Antonio Martinović @ ShadyDevelopment")
                Text("GitHub: https://github.com/ShadyDevelopment")
                Text("Contact: https://github.com/ShadyDevelopment/ECHO-NOW/issues")
                if (configuredAppWeb().isNotBlank()) Text("Project: ${configuredAppWeb()}")
                OutlinedButton(onClick = {
                    dialog = "No release feed is configured for this build, so a remote version check cannot be performed."
                }) { Text("Check for updates") }
                OutlinedButton(onClick = viewModel::reset) { Text("Factory reset") }
            }
        }
            }
            if (maxWidth >= 760.dp) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = maxWidth * .035f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    navigation(Modifier.width(220.dp).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) { settingsContent() }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = maxWidth * .035f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    navigation(Modifier.fillMaxWidth())
                    Box(Modifier.weight(1f).fillMaxWidth()) { settingsContent() }
                }
            }
        }
    }
    dialog?.let { message ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            containerColor = Color(0xFF15151C),
            title = { Text(if (message.startsWith(configuredAppName())) configuredAppName() else "Update status") },
            text = {
                DialogBackdropEffect()
                Text(message)
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done") } },
        )
    }
    colorPicker?.let { target ->
        val selectedColor = when (target) {
            "primary" -> preferences.primaryColor
            "secondary" -> preferences.secondaryColor
            "tertiary" -> preferences.tertiaryColor
            "button-fill" -> preferences.buttonFillColor ?: preferences.primaryColor
            "button-text" -> preferences.buttonTextColor ?: MaterialTheme.colorScheme.onPrimary.asPreferenceColor()
            "button-border" -> preferences.buttonBorderColor ?: MaterialTheme.colorScheme.outline.asPreferenceColor()
            "subtitle-text" -> preferences.subtitleStyle.text
            else -> preferences.subtitleStyle.background
        }
        ColorPickerDialog(
            title = when (target) {
                "primary" -> "Primary color"
                "secondary" -> "Secondary color"
                "tertiary" -> "Third color"
                "button-fill" -> "Button fill color"
                "button-text" -> "Button text color"
                "button-border" -> "Button border color"
                "subtitle-text" -> "Subtitle text color"
                else -> "Subtitle background color"
            },
            selectedColor = selectedColor,
            onDismiss = { colorPicker = null },
            onSelect = { color ->
                when (target) {
                    "primary" -> viewModel.update(preferences.copy(primaryColor = color))
                    "secondary" -> viewModel.update(preferences.copy(secondaryColor = color))
                    "tertiary" -> viewModel.update(preferences.copy(tertiaryColor = color))
                    "button-fill" -> viewModel.update(preferences.copy(buttonFillColor = color))
                    "button-text" -> viewModel.update(preferences.copy(buttonTextColor = color))
                    "button-border" -> viewModel.update(preferences.copy(buttonBorderColor = color))
                    "subtitle-text" -> viewModel.update(
                        preferences.copy(subtitleStyle = preferences.subtitleStyle.copy(text = color)),
                    )
                    else -> viewModel.update(
                        preferences.copy(subtitleStyle = preferences.subtitleStyle.copy(background = color)),
                    )
                }
                colorPicker = null
            },
        )
    }
    if (showPairingDialog) {
        DevicePairingDialog(
            onDismiss = { showPairingDialog = false },
        )
    }
    deviceToUnpair?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToUnpair = null },
            containerColor = Color(0xFF15151C),
            title = { Text("Unpair device") },
            text = {
                DialogBackdropEffect()
                Text("Remove ${device.name} from paired devices?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        unpairDevice(device.id)
                        deviceToUnpair = null
                    },
                ) { Text("Unpair") }
            },
            dismissButton = {
                TextButton(onClick = { deviceToUnpair = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(.35f),
        )
        Text(value, modifier = Modifier.weight(.65f))
    }
}

@Composable
private fun CollapsibleSettingCategory(
    title: String,
    expanded: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                content()
            }
        }
    }
}

@Composable
private fun SettingsCategoryNavigation(
    categories: List<String>,
    selectedCategory: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier,
    horizontal: Boolean,
) {
    if (horizontal) {
        Row(
            modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                if (selectedCategory == category) {
                    Button(onClick = { onSelect(category) }) { Text(category) }
                } else {
                    OutlinedButton(onClick = { onSelect(category) }) { Text(category) }
                }
            }
        }
    } else {
        Column(
            modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEach { category ->
                if (selectedCategory == category) {
                    Button(onClick = { onSelect(category) }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    OutlinedButton(onClick = { onSelect(category) }, modifier = Modifier.fillMaxWidth()) {
                        Text(category, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    selectedColor: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val swatches = listOf(
        0xFFF44336L, 0xFFE91E63L, 0xFF9C27B0L, 0xFF673AB7L, 0xFF3F51B5L,
        0xFF2196F3L, 0xFF03A9F4L, 0xFF00BCD4L, 0xFF009688L, 0xFF4CAF50L,
        0xFF8BC34AL, 0xFFCDDC39L, 0xFFFFEB3BL, 0xFFFFC107L, 0xFFFF9800L,
        0xFFFF5722L, 0xFF795548L, 0xFF9E9E9EL, 0xFF607D8BL, 0xFFFFFFFFL, 0xFF000000L,
        0xFFB71C1CL, 0xFFC2185BL, 0xFF880E4FL, 0xFF6A1B9AL, 0xFF4527A0L,
        0xFF283593L, 0xFF1565C0L, 0xFF0277BDL, 0xFF00838FL, 0xFF00695CL,
        0xFF2E7D32L, 0xFF558B2FL, 0xFF9E9D24L, 0xFFF9A825L, 0xFFFF8F00L,
        0xFFEF6C00L, 0xFFD84315L, 0xFF4E342EL, 0xFF424242L, 0xFF37474FL,
        0xFFFFCDD2L, 0xFFF8BBD0L, 0xFFE1BEE7L, 0xFFD1C4E9L, 0xFFC5CAE9L,
        0xFFBBDEFBL, 0xFFB3E5FCL, 0xFFB2EBF2L, 0xFFB2DFDBL, 0xFFC8E6C9L,
        0xFFDCEDC8L, 0xFFF0F4C3L, 0xFFFFF9C4L, 0xFFFFECB3L, 0xFFFFE0B2L,
        0xFFFFCCBCL, 0xFFD7CCC8L, 0xFFCCCCCCL, 0xFFCFD8DCL,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF15151C),
        title = { Text(title) },
        text = {
            DialogBackdropEffect()
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                swatches.chunked(5).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowColors.forEach { color ->
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        if ((color and 0x00FFFFFFL) == (selectedColor and 0x00FFFFFFL)) 3.dp else 1.dp,
                                        if ((color and 0x00FFFFFFL) == (selectedColor and 0x00FFFFFFL)) {
                                            MaterialTheme.colorScheme.primary
                                        } else MaterialTheme.colorScheme.outline,
                                        CircleShape,
                                    )
                                    .clickable { onSelect(color) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun subtitleFontFamily(name: String): FontFamily = when (name) {
    "Serif" -> FontFamily.Serif
    "Monospace" -> FontFamily.Monospace
    "Cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}
