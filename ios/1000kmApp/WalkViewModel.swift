import Foundation

final class WalkViewModel: ObservableObject {

    // MARK: - Constants
    static let goalDistance: Double = 1000.0
    private static let storageKey = "walkEntries_current"

    // MARK: - Published State
    @Published var entries: [WalkEntry] = []
    @Published var inputKmText: String = ""
    @Published var showResetConfirm: Bool = false

    // MARK: - Computed Properties
    var totalDistance: Double {
        entries.reduce(0) { $0 + $1.distance }
    }

    var remainingDistance: Double {
        max(0, Self.goalDistance - totalDistance)
    }

    var progress: Double {
        min(1.0, totalDistance / Self.goalDistance)
    }

    var goalReached: Bool {
        totalDistance >= Self.goalDistance
    }

    // MARK: - Init
    init() {
        loadEntries()
    }

    // MARK: - Actions

    func addEntry() {
        // Normalize decimal separator for different locales
        let normalized = inputKmText
            .trimmingCharacters(in: .whitespaces)
            .replacingOccurrences(of: ",", with: ".")

        guard let km = Double(normalized), km > 0, km <= 500 else {
            return
        }

        let entry = WalkEntry(distance: km)
        entries.insert(entry, at: 0)
        saveEntries()
        inputKmText = ""
    }

    func deleteEntry(id: UUID) {
        entries.removeAll { $0.id == id }
        saveEntries()
    }

    func resetChallenge() {
        // Archive current entries before clearing
        archiveCurrentEntries()
        entries = []
        saveEntries()
    }

    // MARK: - Persistence

    private func archiveCurrentEntries() {
        guard !entries.isEmpty,
              let data = UserDefaults.standard.data(forKey: Self.storageKey) else { return }
        let timestamp = Int(Date().timeIntervalSince1970)
        let archiveKey = "walkEntries_archive_\(timestamp)"
        UserDefaults.standard.set(data, forKey: archiveKey)
    }

    private func loadEntries() {
        guard let data = UserDefaults.standard.data(forKey: Self.storageKey) else {
            entries = []
            return
        }
        do {
            entries = try JSONDecoder().decode([WalkEntry].self, from: data)
        } catch {
            entries = []
        }
    }

    private func saveEntries() {
        do {
            let data = try JSONEncoder().encode(entries)
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        } catch {
            // Silently fail; data will reload correctly on next launch
        }
    }
}
