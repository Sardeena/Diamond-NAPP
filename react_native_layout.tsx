import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  SafeAreaView,
  ScrollView,
  StatusBar,
  useColorScheme,
} from 'react-native';

// "Sleek Interface" Design Theme Color Palettes
const THEMES = {
  light: {
    background: '#FDF8F6',     // Warm peach/ivory background
    surface: '#FFFFFF',        // Pure white cards
    border: '#E7E0EC',         // Structural grey border
    primary: '#6750A4',        // Brand royal purple
    secondary: '#E8DEF8',      // Soft lavender
    tertiary: '#D1E1FF',       // Ice Blue
    textPrimary: '#1C1B1F',    // Deep charcoal text
    textSecondary: '#49454F',  // Medium slate text
    textMuted: '#625B71',      // Captions
  },
  dark: {
    background: '#1C1B1F',     // Reversed: Deep charcoal dark canvas
    surface: '#2B2930',        // Elevated dark card surface
    border: '#49454F',         // Darker border
    primary: '#D0BCFF',        // Light brand lavender
    secondary: '#4F378B',      // Rich purple container
    tertiary: '#004A77',       // Dark blue container
    textPrimary: '#E6E1E5',    // Light grey text
    textSecondary: '#CAC4D0',  // Muted light grey text
    textMuted: '#938F99',      // Dark slate text
  }
};

export default function DiamondsAppContainer() {
  const systemScheme = useColorScheme();
  const [isDarkMode, setIsDarkMode] = useState(systemScheme === 'dark');
  const theme = isDarkMode ? THEMES.dark : THEMES.light;
  const [activeTab, setActiveTab] = useState('Dashboard');

  const tabs = [
    { name: 'Dashboard', icon: '🏠' },
    { name: 'Bookings', icon: '📅' },
    { name: 'Tours', icon: '✨' },
    { name: 'Clients', icon: '👥' },
    { name: 'Settings', icon: '⚙️' },
  ];

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />

      {/* 1. APP HEADER */}
      <View style={styles.header}>
        <View style={styles.headerInfo}>
          <View style={[styles.avatar, { backgroundColor: theme.primary }]}>
            <Text style={styles.avatarText}>JD</Text>
          </View>
          <View>
            <Text style={[styles.brandSubtitle, { color: theme.textMuted }]}>
              BLUE HORIZON EXCURSIONS
            </Text>
            <Text style={[styles.brandTitle, { color: theme.textPrimary }]}>
              Diamonds Operator
            </Text>
          </View>
        </View>
        <TouchableOpacity 
          style={[styles.themeToggleButton, { backgroundColor: theme.surface, borderColor: theme.border }]}
          onPress={() => setIsDarkMode(!isDarkMode)}
        >
          <Text style={{ fontSize: 18 }}>{isDarkMode ? '☀️' : '🌙'}</Text>
        </TouchableOpacity>
      </View>

      {/* 2. MAIN BENTO GRID VIEWPORT */}
      <ScrollView style={styles.scrollArea} contentContainerStyle={styles.scrollContent}>
        
        {/* Today's Revenue card */}
        <View style={[styles.heroCard, { backgroundColor: theme.primary }]}>
          <Text style={styles.heroLabel}>TODAY'S REVENUE</Text>
          <View style={styles.revenueRow}>
            <Text style={styles.revenueText}>€14,820.50</Text>
            <View style={styles.badgeContainer}>
              <Text style={styles.badgeText}>+12%</Text>
            </View>
          </View>
          <Text style={styles.heroSubText}>42 active VIP bookings right now</Text>
        </View>

        {/* Double-Column Bento Stats */}
        <View style={styles.bentoRow}>
          <View style={[styles.bentoCard, { backgroundColor: theme.secondary }]}>
            <Text style={styles.bentoIcon}>📍</Text>
            <Text style={[styles.bentoValue, { color: theme.textPrimary }]}>08</Text>
            <Text style={[styles.bentoLabel, { color: theme.textSecondary }]}>Live Tours</Text>
          </View>

          <View style={[styles.bentoCard, { backgroundColor: theme.tertiary }]}>
            <Text style={styles.bentoIcon}>📈</Text>
            <Text style={[styles.bentoValue, { color: theme.textPrimary }]}>94%</Text>
            <Text style={[styles.bentoLabel, { color: theme.textSecondary }]}>Fleet Ready</Text>
          </View>
        </View>

        {/* Recent Activity Card */}
        <View style={[styles.recentCard, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <View style={styles.recentHeader}>
            <Text style={[styles.recentTitle, { color: theme.textPrimary }]}>RECENT EXCURSIONS</Text>
            <Text style={[styles.viewAllBtn, { color: theme.primary }]}>View all</Text>
          </View>

          {/* Activity Item 1 */}
          <View style={styles.listItem}>
            <View style={styles.listIconBox}>
              <Text style={{ fontSize: 20 }}>🛥️</Text>
            </View>
            <View style={styles.listDetails}>
              <Text style={[styles.listTitleText, { color: theme.textPrimary }]}>Sunset Yacht Cruise</Text>
              <Text style={[styles.listSubtitleText, { color: theme.textSecondary }]}>Marco Rossi • 4 pax</Text>
            </View>
            <View style={styles.listPriceBox}>
              <Text style={[styles.listPrice, { color: theme.primary }]}>€850</Text>
              <Text style={styles.statusPaid}>PAID</Text>
            </View>
          </View>

          {/* Activity Item 2 */}
          <View style={styles.listItem}>
            <View style={styles.listIconBox}>
              <Text style={{ fontSize: 20 }}>🚁</Text>
            </View>
            <View style={styles.listDetails}>
              <Text style={[styles.listTitleText, { color: theme.textPrimary }]}>Desert Quad Safari</Text>
              <Text style={[styles.listSubtitleText, { color: theme.textSecondary }]}>Sarah Jenkins • 2 pax</Text>
            </View>
            <View style={styles.listPriceBox}>
              <Text style={[styles.listPrice, { color: theme.primary }]}>€240</Text>
              <Text style={styles.statusPending}>PENDING</Text>
            </View>
          </View>
        </View>
      </ScrollView>

      {/* 3. SLEEK NAVIGATION BAR (M3 Safe) */}
      <View style={[styles.bottomNav, { backgroundColor: theme.surface, borderTopColor: theme.border }]}>
        {tabs.map((tab) => {
          const isActive = activeTab === tab.name;
          return (
            <TouchableOpacity
              key={tab.name}
              style={styles.navItem}
              onPress={() => setActiveTab(tab.name)}
            >
              <View style={[
                styles.pillIndicator,
                { backgroundColor: isActive ? theme.secondary : 'transparent' }
              ]}>
                <Text style={styles.navIcon}>{tab.icon}</Text>
              </View>
              <Text style={[
                styles.navLabel,
                { 
                  color: isActive ? theme.textPrimary : theme.textSecondary,
                  fontWeight: isActive ? '700' : '500' 
                }
              ]}>
                {tab.name}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
  },
  headerInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
  avatarText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 16,
  },
  brandSubtitle: {
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 1.5,
  },
  brandTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  themeToggleButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollArea: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
    gap: 16,
  },
  heroCard: {
    borderRadius: 28,
    padding: 24,
    elevation: 2,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
  },
  heroLabel: {
    color: 'rgba(255, 255, 255, 0.8)',
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 2,
  },
  revenueRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    marginTop: 4,
    marginBottom: 8,
    gap: 8,
  },
  revenueText: {
    color: '#FFFFFF',
    fontSize: 34,
    fontWeight: 'bold',
    letterSpacing: -1,
  },
  badgeContainer: {
    backgroundColor: 'rgba(255, 255, 255, 0.25)',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 12,
  },
  badgeText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: 'bold',
  },
  heroSubText: {
    color: 'rgba(255, 255, 255, 0.9)',
    fontSize: 13,
  },
  bentoRow: {
    flexDirection: 'row',
    gap: 16,
  },
  bentoCard: {
    flex: 1,
    borderRadius: 28,
    padding: 20,
    height: 140,
    justifyContent: 'space-between',
  },
  bentoIcon: {
    fontSize: 24,
  },
  bentoValue: {
    fontSize: 28,
    fontWeight: 'bold',
    lineHeight: 34,
  },
  bentoLabel: {
    fontSize: 13,
    fontWeight: '600',
  },
  recentCard: {
    borderRadius: 28,
    borderWidth: 1,
    padding: 20,
  },
  recentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  recentTitle: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 1.5,
  },
  viewAllBtn: {
    fontSize: 13,
    fontWeight: '600',
  },
  listItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F0E7F2',
  },
  listIconBox: {
    width: 44,
    height: 44,
    borderRadius: 14,
    backgroundColor: '#EADDFF',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  listDetails: {
    flex: 1,
  },
  listTitleText: {
    fontSize: 14,
    fontWeight: '700',
  },
  listSubtitleText: {
    fontSize: 12,
    marginTop: 2,
  },
  listPriceBox: {
    alignItems: 'flex-end',
  },
  listPrice: {
    fontSize: 14,
    fontWeight: '700',
  },
  statusPaid: {
    fontSize: 9,
    color: '#10B981',
    fontWeight: 'bold',
    marginTop: 2,
  },
  statusPending: {
    fontSize: 9,
    color: '#F59E0B',
    fontWeight: 'bold',
    marginTop: 2,
  },
  bottomNav: {
    flexDirection: 'row',
    borderTopWidth: 1,
    paddingTop: 8,
    paddingBottom: 24, // High padding for native gesture devices
    paddingHorizontal: 12,
    justifyContent: 'space-around',
    alignItems: 'center',
  },
  navItem: {
    alignItems: 'center',
    gap: 4,
  },
  pillIndicator: {
    paddingHorizontal: 20,
    paddingVertical: 4,
    borderRadius: 16,
  },
  navIcon: {
    fontSize: 20,
  },
  navLabel: {
    fontSize: 11,
  }
});
