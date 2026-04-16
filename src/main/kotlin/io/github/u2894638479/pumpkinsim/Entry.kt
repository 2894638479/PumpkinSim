package io.github.u2894638479.pumpkinsim

import io.github.u2894638479.kotlinmcui.backend.DslEntryService
import io.github.u2894638479.kotlinmcui.backend.createScreen
import io.github.u2894638479.kotlinmcui.backend.showScreen
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.dslBackend
import io.github.u2894638479.kotlinmcui.functions.cached
import io.github.u2894638479.kotlinmcui.functions.dataStore
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.property
import io.github.u2894638479.kotlinmcui.functions.remember
import io.github.u2894638479.kotlinmcui.functions.ui.*
import io.github.u2894638479.kotlinmcui.image.ImageHolder
import io.github.u2894638479.kotlinmcui.math.Axis
import io.github.u2894638479.kotlinmcui.math.Color
import io.github.u2894638479.kotlinmcui.math.Scroller
import io.github.u2894638479.kotlinmcui.math.px
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.minWidth
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import io.github.u2894638479.kotlinmcui.modifier.weight
import io.github.u2894638479.kotlinmcui.modifier.width
import io.github.u2894638479.kotlinmcui.prop.getValue
import io.github.u2894638479.kotlinmcui.prop.setValue
import io.github.u2894638479.kotlinmcui.utils.Config
import io.github.u2894638479.kotlinmcui.utils.Simple
import io.github.u2894638479.kotlinmcui.utils.Simple.simpleTooltip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class Entry : DslEntryService {
    companion object {
        fun blockTexture(name: String) = ImageHolder("minecraft:textures/block/$name.png",16.px,16.px)
    }
    override val icon = blockTexture("pumpkin_side")
    override val id = "pumpkinsim"
    override val name = "Pumpkin Sim"
    override fun initialize() {}

    var matrix = SimMatrix(9,9).apply {
        for(i in array.indices) array[i] = MoistFarmland
        setBlock(4,4, Water)
    }
    enum class Tool(val block: Block) {
        WATER(Water),STEM(Stem),PUMPKIN(Pumpkin),FARMLAND(Farmland),FARMLAND_MOIST(MoistFarmland),NETHERRACK(NetherRack)
    }
    var tool = Tool.STEM
    var running = false
    val scope = CoroutineScope(Dispatchers.Default)
    var avgRunSpeed:Double = Double.NaN
    var showPercentageOnBlock: Boolean = false
    var toBeConsumed = 0
    override fun createScreen() = dslBackend.createScreen {
        Row {
            matrix.ui(tool.block,!running,showPercentageOnBlock)
            val scroller by Scroller.empty.remember.property
            ScrollableColumn(scrollerProp = scroller) {
                if(running) {
                    TextAutoFold { "总步数：${matrix.totalSteps}".emit() }
                    TextAutoFold { "上一步：${matrix.lastHitI},${matrix.lastHitJ}".emit() }
                    TextAutoFold { "总南瓜：${matrix.array.count { it is Pumpkin }}".emit() }
                    TextAutoFold { "最大南瓜数：${matrix.maxPumpkins()}".emit() }
                } else {
                    TextAutoFold { "总步数：${matrix.totalSteps}".emit() }
                    TextAutoFold { "上一步：${matrix.lastHitI},${matrix.lastHitJ}".emit() }
                    TextAutoFold { "总南瓜：${matrix.array.count { it is Pumpkin }}".emit() }
                    TextAutoFold { "最大南瓜数：${matrix.maxPumpkins}".emit() }
                }
                matrix.history.let {
                    TextAutoFold { "已统计轮数：${it.turns}".emit() }
                    TextAutoFold { "待统计：$toBeConsumed".emit() }
                    TextAutoFold { "平均步数：${it.avgSteps}".emit() }
                    TextAutoFold { "平均南瓜数：${it.avgPumpkins}".emit() }
                }
                if(matrix.history.turns > 0) {
                    Simple.Button("查看详细报告") {
                        dslBackend.showScreen {
                            Row {
                                val scroller by Scroller.empty.remember.property
                                val infos by cached(matrix.history to matrix.history.turns) {
                                    matrix.history.pumpkinAvgSteps()
                                }
                                val (avgSteps,achieveRates) = infos
                                Column {
                                    TextAutoFold(Modifier.padding(10.scaled)) { "面积：${matrix.height}x${matrix.width}，总运行步数：${matrix.history.turns}".emit() }
                                    Row(Modifier.padding(3.scaled)) {
                                        Spacer(Modifier.width(32.scaled)) {}
                                        TextFlatten { "平均步数".emit(Color(200,200,255)) }
                                        TextFlatten { "生长速率".emit(Color(100,100,200)) }
                                        TextFlatten { "达成率".emit(Color.GREEN) }
                                    }
                                    ScrollableColumn(Modifier.weight(Double.MAX_VALUE),scroller) {
                                        avgSteps.indices.forEachWithId {
                                            Row(Modifier.padding(3.scaled)) {
                                                Item(Modifier.size(32.scaled,32.scaled),"minecraft:pumpkin",it + 1) {}
                                                val avgStep = avgSteps[it]
                                                TextFlatten { String.format("%.3f",avgStep).emit(Color(200,200,255),18.scaled) }
                                                TextFlatten { String.format("%.3f",(it + 1) / avgStep).emit(Color(100,100,200),18.scaled) }
                                                val achieveRate = achieveRates[it]
                                                TextFlatten {
                                                    String.format("%.3f%%",achieveRate * 100)
                                                        .emit(Color(1 - achieveRate,achieveRate,0.0),18.scaled)
                                                }
                                            }
                                        }
                                    }
                                    Simple.Button("关闭") {
                                        dataStore.onClose()
                                    }
                                }
                                ScrollBar(Modifier.width(10.scaled),scroller,Axis.Vertical) {}
                            }.defaultBackground()
                        }
                    }
                }
                Config.BoolButton(::showPercentageOnBlock,"显示方块百分比")

                val growable = matrix.growable()
                Row {
                    Simple.Button("下一步",!running && growable) {
                        if(matrix.growable()) {
                            matrix.randomTick()
                            matrix.updateMaxPumpkins()
                        }
                    }
                    Simple.Button("下一次生长",!running && growable) {
                        if(matrix.growable()) {
                            val n = matrix.array.count { it is Pumpkin }
                            while(matrix.array.count { it is Pumpkin } == n) {
                                matrix.randomTick()
                            }
                            matrix.updateMaxPumpkins()
                        }
                    }
                }
                Simple.Button("生长点转为泥土",!running) {
                    if(!running) matrix.changeGrowableToDirt()
                }.simpleTooltip("提示") {
                    TextAutoFold(Modifier.minWidth(150.scaled)) {
                        "周围的耕地会为生长加速，而南瓜收割后会变为泥土。把可能生长的位置全变成泥土更接近真实情况。".emit()
                    }
                }
                Row {
                    Simple.Button("重置",!running) {
                        matrix = matrix.spawn()
                        matrix.history = SimMatrix.History()
                    }
                    Simple.Button("运行到结束",!running && growable) {
                        running = true
                        scope.launch {
                            matrix.growAll()
                            running = false
                        }
                    }
                }
                val nThread by Config.Slider(1..<Runtime.getRuntime().availableProcessors(),"最大线程数") {}
                val turns by Config.Slider(1..1000000,"轮数") {}
                Simple.Button("运行${turns}轮",!running) {
                    running = true
                    matrix = matrix.spawn()
                    matrix.history = SimMatrix.History()
                    val queue = ConcurrentLinkedQueue<SimMatrix>()
                    fun totalTurns() = matrix.history.turns
                    val job = scope.launch {
                        val jobs = List(nThread) {
                            val templateMat = matrix.spawn()
                            scope.launch {
                                val templateMat = templateMat.copy()
                                while(true) {
                                    val mat = templateMat.copy().apply { updateAllState() }
                                    mat.growAll()
                                    if(totalTurns() >= turns) break
                                    queue.add(mat)
                                }
                            }
                        }
                        jobs.joinAll()
                    }
                    val job2 = scope.launch {
                        var lastTimeNano = System.nanoTime()
                        var lastTurns = totalTurns()
                        while(true) {
                            val timeNano = System.nanoTime()
                            if(timeNano - lastTimeNano > 1e8) {
                                val turns = totalTurns()
                                avgRunSpeed = (turns - lastTurns).toDouble() / (timeNano - lastTimeNano) * 1e9
                                lastTimeNano = timeNano
                                lastTurns = turns
                                toBeConsumed = queue.size
                            }
                            queue.poll()?.let {
                                it.addToHistory()
                                matrix = it
                            }
                            if(totalTurns() >= turns) break
                        }
                        job.join()
                        toBeConsumed = 0
                        running = false
                    }
                }
                if(avgRunSpeed.isFinite()) Simple.Text { "运行速度：每秒${String.format("%.3f",avgRunSpeed)}轮" }
                Row {
                    Config.EnumButton(::tool,"放置")
                    Box(Modifier.padding(10.scaled,10.scaled).size(32.scaled,32.scaled)) { tool.block.image() }
                }
                var width by Config.Slider(1..50,"宽度") {}
                var height by Config.Slider(1..50,"高度") {}
                Simple.Button("应用尺寸") { matrix = SimMatrix(width,height) }
                Row {
                    Simple.Button("默认布局1") {
                        matrix = SimMatrix(9,9).also {
                            for(i in it.array.indices) {
                                it.array[i] = if(i % 2 == 0) Stem else MoistFarmland
                            }
                            it.setBlock(4,4, Water)
                            it.updateAllState()
                        }
                    }
                    Simple.Button("默认布局2") {
                        matrix = SimMatrix(9,9).also {
                            for(i in it.array.indices) {
                                val j = i % 9
                                it.array[i] = when(j) {
                                    0,3,5,8 -> Stem
                                    else -> MoistFarmland
                                }
                            }
                            it.setBlock(4,4, Water)
                            it.updateAllState()
                        }
                    }
                }
            }
            ScrollBar(Modifier.width(10.scaled),scroller,Axis.Vertical) {}
        }.defaultBackground()
    }
}