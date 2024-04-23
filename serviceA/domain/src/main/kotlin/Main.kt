import Color.GREEN
import Color.RED
import ToyLenses.color
import ToyLenses.name

/*
explain event sourcing and cqrs to a 6 year old kid with simple example like a magical toybox. in addition explain how projections are working and how to keep them up to date
for visualizations use a markdown file with ascii graphic. after that implement that cqrs/es system in kotlin
provide a more enhanced example using sealed classes for events/commands and separate readmodel and writemodel
use a psuh model with a message bus, so that the in-memory projection is always up-to-date
 */

enum class Color {
    RED, BLUE, GREEN, YELLOW
}

data class Toy(val name: String, val color: Color)

@JvmInline
value class ToyId(val value: Int)

sealed class ToyCommand
data class AddToy(val id: ToyId, val toy: Toy) : ToyCommand()
data class RemoveToy(val id: ToyId) : ToyCommand()

sealed class ToyEvent
data class ToyAdded(val id: ToyId, val toy: Toy) : ToyEvent()
data class ToyRemoved(val id: ToyId) : ToyEvent()

fun interface ForStoringEvents {
    fun add(event: ToyEvent)
}

class EventStore : ForStoringEvents {
    private val events = mutableListOf<ToyEvent>()
    // TODO: store is later on used for replays and other things

    override fun add(event: ToyEvent) {
        events.add(event)
    }
}

fun interface ForSubscribing {
    fun subscribe(subscriber: (ToyEvent) -> Unit)
}

fun interface ForPublishing {
    fun publish(event: ToyEvent)
}

class MessageBus : ForSubscribing, ForPublishing {
    private val subscribers = mutableListOf<(ToyEvent) -> Unit>()

    override fun subscribe(subscriber: (ToyEvent) -> Unit) {
        subscribers += subscriber
    }

    override fun publish(event: ToyEvent) {
        subscribers.forEach { subscriber ->
            subscriber(event)
        }
    }
}

class WriteModel(private val eventStore: ForStoringEvents, private val messageBus: ForPublishing) {
    fun execute(command: ToyCommand) {
        handleCommand(command)
    }

    private fun handleCommand(command: ToyCommand) {
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

class ReadModel(messageBus: ForSubscribing) {
    private val history = mutableListOf<ToyEvent>()

    private var currentToysProjection = emptyList<String>()
    private var toysEverSeen = emptyList<String>()
    private var greenToys = emptyList<String>()

    init {
        messageBus.subscribe(this::handle)
    }

    private fun handle(event: ToyEvent) {
        history.add(event)
        currentToysProjection = updateCurrentToys()
        toysEverSeen = updateTyosEverSeen(event)
        greenToys = greenToys(event)
    }

    private fun greenToys(event: ToyEvent) =
        when (event) {
            is ToyAdded -> if (color(event) == GREEN) greenToys + name(event) else greenToys
            is ToyRemoved -> greenToys
        }

    private fun updateTyosEverSeen(event: ToyEvent) =
        when (event) {
            is ToyAdded -> (toysEverSeen + name(event)).distinct()
            is ToyRemoved -> toysEverSeen
        }

    private fun updateCurrentToys(): List<String> =
        history
            .fold(mapOf<ToyId, Toy>()) { state, event ->
                when (event) {
                    is ToyAdded -> state + (event.id to event.toy)
                    is ToyRemoved -> state - event.id
                }
            }
            .values
            .map(Toy::name)

    fun currentToys(): List<String> = currentToysProjection

    fun toysEverSeen(): List<String> = toysEverSeen

    fun allGreenToys(): List<String> = greenToys
}

object ToyLenses {
    fun color(event: ToyAdded) = event.toy.color

    fun name(event: ToyAdded) = event.toy.name
}

fun main() {
    val messageBus = MessageBus()
    val eventStore = EventStore()
    val commandHandler = WriteModel(eventStore, messageBus)
    val readModel = ReadModel(messageBus)

    with(commandHandler) {
        execute(AddToy(ToyId(1), Toy("3D Toy", RED)))
        execute(AddToy(ToyId(2), Toy("4D Toy", GREEN)))

        execute(RemoveToy(ToyId(1)))

        execute(AddToy(ToyId(3), Toy("car", Color.BLUE)))
        execute(AddToy(ToyId(4), Toy("bear", RED)))

        execute(RemoveToy(ToyId(3)))
    }

    println("Toys ever seen: ${readModel.toysEverSeen()}")
    println("Current Toys: ${readModel.currentToys()}")
    println("Green Toys: ${readModel.allGreenToys()}")
}
