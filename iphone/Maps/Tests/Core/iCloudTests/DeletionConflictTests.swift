@testable import Organic_Maps__Debug_
import XCTest

/// Test cases for deletion tracking and conflict resolution edge cases
final class DeletionConflictTests: XCTestCase {
  var syncStateManager: SynchronizationStateResolver!
  var outgoingEvents: [OutgoingSynchronizationEvent] = []

  override func setUp() {
    super.setUp()
    syncStateManager = iCloudSynchronizationStateResolver(isInitialSynchronization: false)
  }

  override func tearDown() {
    syncStateManager = nil
    outgoingEvents.removeAll()
    super.tearDown()
  }

  // MARK: - Edit + Delete Conflict Tests

  /// Edge Case: Device A edits item, Device B deletes item
  /// Expected: If deletion is newer than edit, delete; otherwise preserve edit
  func testEditAndDeleteConflict_DeletionNewer() {
    // Setup: Item edited at 15:00, deleted at 15:30
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500)  // Edit at 15:00
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1530)  // Deleted at 15:30
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify deletion wins because it's newer
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .removeLocalItem(let item) = event, item == localItem {
          return true
        }
        return false
      },
      "Expected item to be deleted since deletion is newer than edit"
    )
  }

  /// Edge Case: Item deleted at 15:00, then edited at 15:30
  /// Expected: Preserve the edit over the deletion
  func testEditAndDeleteConflict_EditNewer() {
    // Setup: Item deleted at 15:00, edited at 15:30
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1530)  // Edit at 15:30 (newer)
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1500)  // Deleted at 15:00 (older)
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify edit wins and is synced to cloud
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .updateCloudItem(let item) = event, item == localItem {
          return true
        }
        return false
      },
      "Expected local edit to be synced to cloud when newer than deletion"
    )
  }

  // MARK: - Multiple Device Deletion Tests

  /// Both devices delete the same item independently
  /// Expected: Item stays deleted, no sync conflicts
  func testBothDevicesDelete() {
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1520)
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1530)
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify no sync actions needed - both already deleted
    XCTAssertTrue(
      outgoingEvents.isEmpty,
      "Expected no events when both items are deleted"
    )
  }

  // MARK: - Local Deletion Propagation Tests

  /// Device A deletes item locally, Device B has updated it in cloud
  /// Expected: Respect local deletion if it's newer
  func testLocalDeleteIsNewer() {
    // Setup: Cloud has update at 15:00, local deletion at 15:30
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1530)  // Deleted locally at 15:30
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500)  // Cloud edit at 15:00
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify local deletion is propagated to cloud
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .removeCloudItem(let item) = event, item == cloudItem {
          return true
        }
        return false
      },
      "Expected local deletion to be propagated to cloud when newer"
    )
  }

  /// Device A has old cloud item deleted locally, Device B edits it in cloud
  /// Expected: Restore from cloud since cloud edit is newer
  func testCloudEditAfterLocalDelete() {
    // Setup: Local deletion at 15:00, cloud edit at 15:30
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1500)  // Deleted locally at 15:00
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1530)  // Edited in cloud at 15:30
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify restoration from cloud
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .updateLocalItem(let item) = event, item == cloudItem {
          return true
        }
        return false
      },
      "Expected cloud item to be restored locally when cloud edit is newer than local deletion"
    )
  }

  // MARK: - Cloud Deletion Propagation Tests

  /// Cloud deletes item after local edit
  /// Expected: Honor cloud deletion since it's newer
  func testCloudDeleteAfterLocalEdit() {
    // Setup: Local edit at 15:00, cloud deletion at 15:30
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500)  // Edited locally at 15:00
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1530)  // Deleted in cloud at 15:30
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify deletion is honored
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .removeLocalItem(let item) = event, item == localItem {
          return true
        }
        return false
      },
      "Expected local item to be deleted when cloud deletion is newer"
    )
  }

  /// Cloud has old deletion, local item still exists (never deleted locally)
  /// Expected: Treat as two independent items (one deleted, one not)
  func testCloudDeletedLocalNotDeleted() {
    // Setup: Cloud has deletion, local has active item
    let localItem = LocalMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1500)
    )
    let cloudItem = CloudMetadataItem.stub(
      fileName: "Paris_Restaurant",
      lastModificationDate: TimeInterval(1400),
      isDeleted: true,
      deletionTimestamp: TimeInterval(1400)
    )

    let localItems = LocalContents([localItem])
    let cloudItems = CloudContents([cloudItem])

    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringLocalContents(localItems))
    outgoingEvents = syncStateManager.resolveEvent(.didFinishGatheringCloudContents(cloudItems))

    // Verify local item is updated to cloud (restoration after cloud deletion)
    XCTAssertTrue(
      outgoingEvents.contains { event in
        if case .updateCloudItem(let item) = event, item == localItem {
          return true
        }
        return false
      },
      "Expected local item to restore cloud item when local edit is newer than cloud deletion"
    )
  }
}

