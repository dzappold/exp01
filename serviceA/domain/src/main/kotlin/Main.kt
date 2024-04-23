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
    // TODO: store is later on used for replays and other things

    fun add(event: ToyEvent) {
        events.add(event)
    }
}

class MessageBus {
    private val subscribers = mutableListOf<(ToyEvent) -> Unit>()

    fun subscribe(subscriber: (ToyEvent) -> Unit) {
        subscribers += subscriber
    }

    fun publish(event: ToyEvent) {
        subscribers.forEach { subscriber ->
            subscriber(event)
        }
    }
}

class WriteModel(private val eventStore: EventStore, private val messageBus: MessageBus) {
    fun execute(command: ToyCommand) {
        handleCommand(command)
    }

    fun handleCommand(command: ToyCommand) {
        when (command) {
            is AddToy -> handleAddToy(command)
            is RemoveToy -> handleRemoveToy(command)
        }
    }

    private fun handleAddToy(command: AddToy) {
        ToyAdded(command.id, command.toy)
            .also(eventStore::add)
            .also(messageBus::publish)
    }

    private fun handleRemoveToy(command: RemoveToy) {
        ToyRemoved(command.id)
            .also(eventStore::add)
            .also(messageBus::publish)
    }

}

class ReadModel(messageBus: MessageBus) {
    private val history = mutableListOf<ToyEvent>()
    init {
        messageBus.subscribe(this::handle)
    }

    private fun handle(event: ToyEvent) {
        history.add(event)
    }

    fun currentToys(): List<String> {
        return history
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
        return history.filterIsInstance<ToyAdded>().map { it.toy.name }
    }
}

fun main() {
    val messageBus = MessageBus()
    val eventStore = EventStore()
    val commandHandler = WriteModel(eventStore, messageBus)
    val readModel = ReadModel(messageBus)

    with(commandHandler) {
        execute(AddToy(ToyId(1), Toy("3D Toy")))
        execute(AddToy(ToyId(2), Toy("4D Toy")))

        execute(RemoveToy(ToyId(1)))

        execute(AddToy(ToyId(3), Toy("car")))
        execute(AddToy(ToyId(4), Toy("bear")))

        execute(RemoveToy(ToyId(3)))
    }

    println("Toys ever seen: ${readModel.toysEverSeen()}")
    println("Current Toys: ${readModel.currentToys()}")
}
