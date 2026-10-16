import kotlin.random.Random

abstract class Character(
    val name: String,
    val role: String,
    var hp: Int,
    val attack: Int,
    var spUses: Int
) {
    val maxHp: Int = hp

    fun isAlive(): Boolean {
        return hp > 0
    }

    fun takeDamage(damage: Int) {
        hp = hp - damage
        if (hp <= 0) {
            println("$name ($role) получает $damage урона и погибает!")
        } else {
            println("$name ($role) получает $damage урона. Осталось HP: $hp/$maxHp")
        }
    }

    abstract fun act(enemies: List<Character>, allies: List<Character>)
}

class Warrior(name: String) : Character(name, "Воин", 100, 20, 2) {
    override fun act(enemies: List<Character>, allies: List<Character>) {
        val target = enemies.random()
        if (spUses > 0 && Random.nextBoolean()) {
            val dmg = attack * 2
            spUses--
            println("$name использует Критический удар! Наносит $dmg урона по ${target.name}.")
            target.takeDamage(dmg)
        } else {
            println("$name атакует обычным ударом ${target.name}. Наносит $attack урона.")
            target.takeDamage(attack)
        }
    }
}

class Mage(name: String) : Character(name, "Маг", 60, 15, 1) {
    override fun act(enemies: List<Character>, allies: List<Character>) {
        if (spUses > 0) {
            println("$name применяет Огненный дождь по всем врагам!")
            spUses--
            for (enemy in enemies) {
                enemy.takeDamage(attack)
            }
        } else {
            val target = enemies.random()
            println("$name пускает искру в ${target.name}. Наносит $attack урона.")
            target.takeDamage(attack)
        }
    }
}

class Healer(name: String) : Character(name, "Лекарь", 80, 5, 2) {
    override fun act(enemies: List<Character>, allies: List<Character>) {
        val wounded = allies.filter { it.hp < it.maxHp }
        if (spUses > 0 && wounded.isNotEmpty()) {
            val target = wounded.random()
            target.hp = target.hp + 30
            if (target.hp > target.maxHp) {
                target.hp = target.maxHp
            }
            spUses--
            println("$name лечит ${target.name} на 30 HP. Осталось HP: ${target.hp}/${target.maxHp}")
        } else {
            val target = enemies.random()
            println("$name бьет посохом ${target.name}. Наносит $attack урона.")
            target.takeDamage(attack)
        }
    }
}

fun main() {
    val squad1 = listOf(Warrior("Алекс"), Mage("Мерлин"), Healer("Светлана"))
    val squad2 = listOf(Warrior("Брут"), Mage("Гулдан"), Healer("Медик"))

    println("НАЧАЛО БОЯ")
    var round = 1

    while (round <= 10) {
        println()
        println("Раунд $round")

        val alive1 = squad1.filter { it.isAlive() }
        val alive2 = squad2.filter { it.isAlive() }

        if (alive1.isEmpty() || alive2.isEmpty()) {
            break
        }

        val all = (alive1 + alive2).shuffled()

        for (p in all) {
            if (!p.isAlive()) {
                continue
            }

            var enemies = squad2.filter { it.isAlive() }
            var allies = squad1.filter { it.isAlive() }

            if (p in squad2) {
                enemies = squad1.filter { it.isAlive() }
                allies = squad2.filter { it.isAlive() }
            }

            if (enemies.isEmpty()) {
                break
            }

            p.act(enemies, allies)
        }
        round++
    }

    println()
    println("РЕЗУЛЬТАТ БОЯ")
    val final1 = squad1.filter { it.isAlive() }
    val final2 = squad2.filter { it.isAlive() }

    if (final1.isNotEmpty() && final2.isEmpty()) {
        println("Победил Отряд 1")
    } else if (final2.isNotEmpty() && final1.isEmpty()) {
        println("Победил Отряд 2")
    } else {
        println("Ничья!")
    }
}