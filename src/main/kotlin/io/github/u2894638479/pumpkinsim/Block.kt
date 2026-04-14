package io.github.u2894638479.pumpkinsim

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.functions.decorator.rotate
import io.github.u2894638479.kotlinmcui.functions.ui.Box
import io.github.u2894638479.kotlinmcui.functions.ui.ColorRect
import io.github.u2894638479.kotlinmcui.functions.ui.Image
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.scope.DslChild
import io.github.u2894638479.pumpkinsim.Entry.Companion.blockTexture
import kotlin.math.PI

sealed interface Block {
    context(ctx: DslContext)
    fun image(): DslChild
}

interface NeedWaterBlock : Block {
    val replace: Block get() = Dirt
}
interface StemBlock : Block
interface PumpkinGrowableBlock : Block
interface GrowAccelerateBlock : Block {
    val accelerateRate get() = 3.0
}
interface GrownStemBlock : StemBlock, GrowAccelerateBlock, NeedWaterBlock {
    val rad: Double
    context(ctx: DslContext)
    override fun image() = Box {
        Image(Modifier, blockTexture("pumpkin_stem"), Color(200,200,0)) {}.rotate(rad)
        Image(Modifier, blockTexture("farmland_moist")) {}
    }
}

object Stem : StemBlock, GrowAccelerateBlock, NeedWaterBlock {
    context(ctx: DslContext)
    override fun image() = Box {
        Image(Modifier, blockTexture("pumpkin_stem"), Color.GREEN) {}
        Image(Modifier, blockTexture("farmland_moist")) {}
    }
}

object GrownStemL : GrownStemBlock { override val rad = -PI/2 }
object GrownStemT : GrownStemBlock { override val rad = 0.0 }
object GrownStemR : GrownStemBlock { override val rad = PI/2 }
object GrownStemB : GrownStemBlock { override val rad = PI }
object NetherRack : Block {
    context(ctx: DslContext)
    override fun image() = Image(Modifier,blockTexture("netherrack")) {}
}
object Water : Block {
    context(ctx: DslContext)
    override fun image() = ColorRect(Modifier,Color.BLUE) {}
}
object Dirt : PumpkinGrowableBlock {
    context(ctx: DslContext)
    override fun image() = Image(Modifier,blockTexture("dirt")) {}
}
object Farmland : GrowAccelerateBlock, PumpkinGrowableBlock, NeedWaterBlock {
    override val accelerateRate get() = 1.0
    context(ctx: DslContext)
    override fun image() = Image(Modifier,blockTexture("farmland")) {}
}
object MoistFarmland : GrowAccelerateBlock, PumpkinGrowableBlock, NeedWaterBlock {
    context(ctx: DslContext)
    override fun image() = Image(Modifier,blockTexture("farmland_moist")) {}
}
object Pumpkin : Block {
    context(ctx: DslContext)
    override fun image() = Image(Modifier,blockTexture("pumpkin_top")) {}
}

