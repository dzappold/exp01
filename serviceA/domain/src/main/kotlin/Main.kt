/*
explain event sourcing and cqrs to a 6 year old kid with simple example like a magical toybox. in addition explain how projections are working and how to keep them up to date
for visualizations use a markdown file with ascii graphic. after that implement that cqrs/es system in kotlin
provide a more enhanced example using sealed classes for events/commands and separate readmodel and writemodel
use a psuh model with a message bus, so that the in-memory projection is always up-to-date
 */

data class Toy(val name: String)

@JvmInline
value class ToyId(val value: Int)

sealed class ToyCommand
data class AddToy(val id: ToyId, val toy: Toy) : ToyCommand()
data class RemoveToy(val id: ToyId) : ToyCommand()

sealed class ToyEvent
data class ToyAdded(val id: ToyId, val toy: Toy) : ToyEvent()
data class ToyRemoved(val id: ToyId) : ToyEvent()

class EventStore {
    private val events = mutableListOf<ToyEvent>()
    val eventStream: List<ToyEvent>
        get() = events.toList()

    fun handleCommand(command: ToyCommand) {
        when (command) {
            is AddToy -> handleAddToy(command)
            is RemoveToy -> handleRemoveToy(command)
        }
    }

    private fun handleAddToy(command: AddToy) {
        events.add(ToyAdded(command.id, command.toy))
    }

    private fun handleRemoveToy(command: RemoveToy) {
        events.add(ToyRemoved(command.id))
    }
}

class WriteModel(private val eventStore: EventStore) {
    fun execute(command: ToyCommand) {
        eventStore.handleCommand(command)
    }
}

class ReadModel(private val eventStore: EventStore) {
    fun currentToys(): List<String> {
        val eventStream = mutableMapOf<ToyId, Toy>()
        eventStore.eventStream
            .forEach { toyEvent ->
                when (toyEvent) {
                    is ToyAdded -> eventStream[toyEvent.id] = toyEvent.toy
                    is ToyRemoved -> eventStream.remove(toyEvent.id)
                }
            }

        return eventStream.values.map { it.name }
    }

    fun currentToysUsingFold(): List<String> {
        return eventStore.eventStream
            .fold(mutableMapOf<ToyId, Toy>()) { state, event ->
                when (event) {
                    is ToyAdded -> state[event.id] = event.toy
                    is ToyRemoved -> state.remove(event.id)
                }
                state
            }
            .values.map(Toy::name)
    }

    fun currentToys2(): List<String> {
        return eventStore.eventStream
            .fold(mapOf<ToyId, Toy>()) { state, event ->
                when (event) {
                    is ToyAdded -> state + (event.id to event.toy)
                    is ToyRemoved -> state - event.id
                }
            }
            .values
            .map(Toy::name)
    }

    fun toysEverSeen(): List<String> {
        return eventStore.eventStream.filterIsInstance<ToyAdded>().map { it.toy.name }
    }
}

fun main() {
    val eventStore = EventStore()
    val commandHandler = WriteModel(eventStore)

    with(commandHandler) {
        execute(AddToy(ToyId(1), Toy("3D Toy")))
        execute(AddToy(ToyId(2), Toy("4D Toy")))
        execute(RemoveToy(ToyId(1)))
        execute(AddToy(ToyId(3), Toy("car")))
        execute(AddToy(ToyId(4), Toy("bear")))
        execute(RemoveToy(ToyId(3)))
    }

    val readModel = ReadModel(eventStore)
    println("Toys ever seen: ${readModel.toysEverSeen()}")
    println("Current Toys: ${readModel.currentToys()}")
    println("Current Toys: ${readModel.currentToysUsingFold()}")
    println("Current Toys: ${readModel.currentToys2()}")
}
