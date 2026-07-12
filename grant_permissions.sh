#!/bin/bash
# DarkSettings - Grant all special permissions via ADB
# Run this after installing the APK

PACKAGE="com.darksettings"

echo "=== DarkSettings Permission Grant Script ==="
echo ""

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo "ERROR: ADB not found. Install Android SDK Platform Tools."
    exit 1
fi

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "ERROR: No device connected. Connect via USB and enable USB debugging."
    exit 1
fi

echo "Granting permissions for $PACKAGE..."
echo ""

# Grant all runtime permissions
PERMISSIONS=(
    "android.permission.ACCESS_FINE_LOCATION"
    "android.permission.ACCESS_COARSE_LOCATION"
    "android.permission.ACCESS_BACKGROUND_LOCATION"
    "android.permission.BLUETOOTH"
    "android.permission.BLUETOOTH_ADMIN"
    "android.permission.BLUETOOTH_CONNECT"
    "android.permission.BLUETOOTH_SCAN"
    "android.permission.READ_PHONE_STATE"
    "android.permission.CAMERA"
    "android.permission.RECORD_AUDIO"
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.NFC"
    "android.permission.INTERNET"
    "android.permission.ACCESS_NETWORK_STATE"
    "android.permission.CHANGE_NETWORK_STATE"
    "android.permission.ACCESS_WIFI_STATE"
    "android.permission.CHANGE_WIFI_STATE"
    "android.permission.CHANGE_WIFI_MULTICAST_STATE"
    "android.permission.WAKE_LOCK"
    "android.permission.RECEIVE_BOOT_COMPLETED"
    "android.permission.FOREGROUND_SERVICE"
    "android.permission.BATTERY_STATS"
    "android.permission.PACKAGE_USAGE_STATS"
    "android.permission.QUERY_ALL_PACKAGES"
    "android.permission.MANAGE_EXTERNAL_STORAGE"
    "android.permission.READ_MEDIA_IMAGES"
    "android.permission.READ_MEDIA_VIDEO"
    "android.permission.READ_MEDIA_AUDIO"
    "android.permission.POST_NOTIFICATIONS"
    "android.permission.READ_CONTACTS"
    "android.permission.WRITE_CONTACTS"
    "android.permission.READ_CALENDAR"
    "android.permission.WRITE_CALENDAR"
    "android.permission.READ_CALL_LOG"
    "android.permission.WRITE_CALL_LOG"
    "android.permission.PROCESS_OUTGOING_CALLS"
    "android.permission.READ_SMS"
    "android.permission.SEND_SMS"
    "android.permission.RECEIVE_SMS"
    "android.permission.RECEIVE_MMS"
    "android.permission.RECEIVE_WAP_PUSH"
    "android.permission.VIBRATE"
    "android.permission.USE_FINGERPRINT"
    "android.permission.USE_BIOMETRIC"
    "android.permission.BODY_SENSORS"
    "android.permission.ACTIVITY_RECOGNITION"
    "android.permission.READ_PHONE_NUMBERS"
    "android.permission.ANSWER_PHONE_CALLS"
    "android.permission.CALL_PHONE"
    "android.permission.ACCEPT_HANDOVER"
    "android.permission.MEDIA_LOCATION"
    "android.permission.READ_USER_DICTIONARY"
    "android.permission.WRITE_USER_DICTIONARY"
    "android.permission.READ_SOCIAL_STREAM"
    "android.permission.WRITE_SOCIAL_STREAM"
    "android.permission.READ_PROFILE"
    "android.permission.WRITE_PROFILE"
    "android.permission.READOWNEREDUPGRADABLES"
    "android.permission.MANAGE_OWN_CALLS"
)

for perm in "${PERMISSIONS[@]}"; do
    echo -n "Granting $perm... "
    result=$(adb shell pm grant "$PACKAGE" "$perm" 2>&1)
    if [ $? -eq 0 ]; then
        echo "OK"
    else
        echo "FAILED (may need different permission or already granted)"
    fi
done

echo ""
echo "=== Granting special permissions ==="
echo ""

# Grant WRITE_SECURE_SETTINGS
echo -n "Granting WRITE_SECURE_SETTINGS... "
adb shell pm grant "$PACKAGE" android.permission.WRITE_SECURE_SETTINGS 2>&1
echo ""

# Grant WRITE_SETTINGS
echo -n "Granting WRITE_SETTINGS... "
adb shell appops set "$PACKAGE" android:write_settings allow 2>&1
echo ""

# Grant MANAGE_EXTERNAL_STORAGE
echo -n "Granting MANAGE_EXTERNAL_STORAGE... "
adb shell appops set "$PACKAGE" android:manage_external_storage allow 2>&1
echo ""

# Grant PACKAGE_USAGE_STATS
echo -n "Granting PACKAGE_USAGE_STATS... "
adb shell appops set "$PACKAGE" android:access_media_location allow 2>&1
echo ""

# Grant DUMP
echo -n "Granting DUMP... "
adb shell pm grant "$PACKAGE" android.permission.DUMP 2>&1
echo ""

# Grant INSTALL_PACKAGES
echo -n "Granting INSTALL_PACKAGES... "
adb shell pm grant "$PACKAGE" android.permission.INSTALL_PACKAGES 2>&1
echo ""

# Grant DELETE_PACKAGES
echo -n "Granting DELETE_PACKAGES... "
adb shell pm grant "$PACKAGE" android.permission.DELETE_PACKAGES 2>&1
echo ""

# Grant DEVICE_POWER
echo -n "Granting DEVICE_POWER... "
adb shell pm grant "$PACKAGE" android.permission.DEVICE_POWER 2>&1
echo ""

# Grant REBOOT
echo -n "Granting REBOOT... "
adb shell pm grant "$PACKAGE" android.permission.REBOOT 2>&1
echo ""

# Grant STATUS_BAR
echo -n "Granting STATUS_BAR... "
adb shell pm grant "$PACKAGE" android.permission.STATUS_BAR 2>&1
echo ""

# Grant EXPAND_STATUS_BAR
echo -n "Granting EXPAND_STATUS_BAR... "
adb shell pm grant "$PACKAGE" android.permission.EXPAND_STATUS_BAR 2>&1
echo ""

# Grant CONNECTIVITY_INTERNAL
echo -n "Granting CONNECTIVITY_INTERNAL... "
adb shell pm grant "$PACKAGE" android.permission.CONNECTIVITY_INTERNAL 2>&1
echo ""

# Grant MANAGE_NETWORK_POLICY
echo -n "Granting MANAGE_NETWORK_POLICY... "
adb shell pm grant "$PACKAGE" android.permission.MANAGE_NETWORK_POLICY 2>&1
echo ""

# Grant TETHER_PRIVILEGED
echo -n "Granting TETHER_PRIVILEGED... "
adb shell pm grant "$PACKAGE" android.permission.TETHER_PRIVILEGED 2>&1
echo ""

# Grant NETWORK_SETTINGS
echo -n "Granting NETWORK_SETTINGS... "
adb shell pm grant "$PACKAGE" android.permission.NETWORK_SETTINGS 2>&1
echo ""

# Grant MODIFY_PHONE_STATE
echo -n "Granting MODIFY_PHONE_STATE... "
adb shell pm grant "$PACKAGE" android.permission.MODIFY_PHONE_STATE 2>&1
echo ""

# Grant SET_TIME
echo -n "Granting SET_TIME... "
adb shell pm grant "$PACKAGE" android.permission.SET_TIME 2>&1
echo ""

# Grant SET_TIME_ZONE
echo -n "Granting SET_TIME_ZONE... "
adb shell pm grant "$PACKAGE" android.permission.SET_TIME_ZONE 2>&1
echo ""

# Grant WRITE_MEDIA_STORAGE
echo -n "Granting WRITE_MEDIA_STORAGE... "
adb shell pm grant "$PACKAGE" android.permission.WRITE_MEDIA_STORAGE 2>&1
echo ""

# Grant CLEAR_APP_USER_DATA
echo -n "Granting CLEAR_APP_USER_DATA... "
adb shell pm grant "$PACKAGE" android.permission.CLEAR_APP_USER_DATA 2>&1
echo ""

# Grant FORCE_STOP_PACKAGES
echo -n "Granting FORCE_STOP_PACKAGES... "
adb shell pm grant "$PACKAGE" android.permission.FORCE_STOP_PACKAGES 2>&1
echo ""

echo ""
echo "=== Done! ==="
echo "All permissions have been granted."
echo "Open DarkSettings app to start using it."
echo ""
echo "Note: Some permissions may require device owner or system app status."
echo "If a permission failed, it may not be available on your device/Android version."
