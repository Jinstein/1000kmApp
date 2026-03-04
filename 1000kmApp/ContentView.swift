import SwiftUI

// MARK: - Root View

struct ContentView: View {
    @StateObject private var viewModel = WalkViewModel()
    @FocusState private var isInputFocused: Bool

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 28) {
                    ProgressRingView(
                        progress: viewModel.progress,
                        totalKm: viewModel.totalDistance,
                        goalReached: viewModel.goalReached
                    )
                    .padding(.top, 16)

                    StatCardsView(
                        totalKm: viewModel.totalDistance,
                        remainingKm: viewModel.remainingDistance
                    )

                    InputRowView(
                        text: $viewModel.inputKmText,
                        isFocused: $isInputFocused,
                        onAdd: {
                            viewModel.addEntry()
                            isInputFocused = false
                        }
                    )

                    EntryListView(
                        entries: viewModel.entries,
                        onDelete: { id in
                            viewModel.deleteEntry(id: id)
                        }
                    )

                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 20)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("1000km 도전")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.showResetConfirm = true
                    } label: {
                        Image(systemName: "arrow.counterclockwise.circle")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .alert("새 도전 시작", isPresented: $viewModel.showResetConfirm) {
                Button("취소", role: .cancel) {}
                Button("초기화", role: .destructive) {
                    viewModel.resetChallenge()
                }
            } message: {
                Text("현재 기록이 모두 초기화됩니다.\n이전 기록은 별도로 저장됩니다.")
            }
        }
        .onTapGesture {
            isInputFocused = false
        }
    }
}

// MARK: - Progress Ring

struct ProgressRingView: View {
    let progress: Double
    let totalKm: Double
    let goalReached: Bool

    private let ringDiameter: CGFloat = 220
    private let lineWidth: CGFloat = 20

    var ringColor: Color {
        goalReached ? .green : .blue
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color(.systemGray5), lineWidth: lineWidth)

            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    ringColor,
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeOut(duration: 0.5), value: progress)

            VStack(spacing: 6) {
                if goalReached {
                    Text("목표 달성!")
                        .font(.title2.bold())
                        .foregroundColor(.green)
                    Text("🎉")
                        .font(.system(size: 36))
                } else {
                    Text(String(format: "%.1f km", totalKm))
                        .font(.system(size: 34, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)
                        .monospacedDigit()
                    Text(String(format: "%.1f%%", progress * 100))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .frame(width: ringDiameter, height: ringDiameter)
    }
}

// MARK: - Stat Cards

struct StatCardsView: View {
    let totalKm: Double
    let remainingKm: Double

    var body: some View {
        HStack(spacing: 12) {
            StatCard(
                label: "총 거리",
                value: String(format: "%.1f", totalKm),
                unit: "km",
                color: .blue,
                icon: "figure.walk"
            )
            StatCard(
                label: "남은 거리",
                value: String(format: "%.1f", remainingKm),
                unit: "km",
                color: .orange,
                icon: "flag.checkered"
            )
        }
    }
}

struct StatCard: View {
    let label: String
    let value: String
    let unit: String
    let color: Color
    let icon: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.system(.title2, design: .rounded).bold())
                    .foregroundColor(.primary)
                    .monospacedDigit()
                Text(unit)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }
}

// MARK: - Input Row

struct InputRowView: View {
    @Binding var text: String
    var isFocused: FocusState<Bool>.Binding
    let onAdd: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("오늘 걸은 거리 추가")
                .font(.headline)
                .foregroundColor(.primary)

            HStack(spacing: 10) {
                TextField("0.0", text: $text)
                    .keyboardType(.decimalPad)
                    .focused(isFocused)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .font(.system(.title3, design: .rounded))
                    .frame(maxWidth: 110)

                Text("km")
                    .font(.title3)
                    .foregroundColor(.secondary)

                Spacer()

                Button(action: onAdd) {
                    Text("추가")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }
}

// MARK: - Entry List

struct EntryListView: View {
    let entries: [WalkEntry]
    let onDelete: (UUID) -> Void

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateStyle = .medium
        f.timeStyle = .short
        f.locale = Locale(identifier: "ko_KR")
        return f
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("최근 기록")
                    .font(.headline)
                Spacer()
                Text("총 \(entries.count)회")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            if entries.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "figure.walk.circle")
                        .font(.system(size: 44))
                        .foregroundColor(.secondary.opacity(0.5))
                    Text("아직 기록이 없습니다\n오늘 걸은 거리를 추가해보세요!")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 36)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(16)
            } else {
                LazyVStack(spacing: 0) {
                    ForEach(entries) { entry in
                        EntryRowView(
                            entry: entry,
                            formatter: Self.dateFormatter,
                            onDelete: onDelete
                        )
                        if entry.id != entries.last?.id {
                            Divider()
                                .padding(.leading, 16)
                        }
                    }
                }
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(16)
            }
        }
    }
}

struct EntryRowView: View {
    let entry: WalkEntry
    let formatter: DateFormatter
    let onDelete: (UUID) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(formatter.string(from: entry.date))
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Text(String(format: "+%.1f km", entry.distance))
                .font(.system(.body, design: .rounded).weight(.semibold))
                .foregroundColor(.blue)
                .monospacedDigit()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                onDelete(entry.id)
            } label: {
                Label("삭제", systemImage: "trash")
            }
        }
    }
}

// MARK: - Preview

#Preview {
    ContentView()
}
