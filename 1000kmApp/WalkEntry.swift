import Foundation

struct WalkEntry: Identifiable, Codable {
    let id: UUID
    let date: Date
    let distance: Double  // km

    init(id: UUID = UUID(), date: Date = Date(), distance: Double) {
        self.id = id
        self.date = date
        self.distance = distance
    }
}
