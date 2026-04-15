package io.github.u2894638479.pumpkinsim

import io.github.u2894638479.kotlinmcui.context.DslContext
import io.github.u2894638479.kotlinmcui.context.scaled
import io.github.u2894638479.kotlinmcui.functions.decorator.clickable
import io.github.u2894638479.kotlinmcui.functions.decorator.hoverMask
import io.github.u2894638479.kotlinmcui.functions.forEachWithId
import io.github.u2894638479.kotlinmcui.functions.ui.Column
import io.github.u2894638479.kotlinmcui.functions.ui.Row
import io.github.u2894638479.kotlinmcui.math.Measure
import io.github.u2894638479.kotlinmcui.modifier.Modifier
import io.github.u2894638479.kotlinmcui.modifier.padding
import io.github.u2894638479.kotlinmcui.modifier.size
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class SimMatrix(val width:Int,val height:Int,var history:History = History()) {
    class History {
        val stepsLock = Any()
        val pumpkinsLock = Any()
        val pumpkinStepsLock = Any()
        var turns = 0
        val totalSteps = mutableListOf<Int>()
        val avgSteps get() = synchronized(stepsLock) { totalSteps.average() }
        val pumpkinSteps = mutableListOf<IntArray>()
        val totalPumpkins = mutableListOf<Int>()
        val avgPumpkins get() = synchronized(pumpkinsLock) { totalPumpkins.average() }
        fun pumpkinAvgSteps(): Pair<List<Double>,List<Double>> {
            synchronized(pumpkinStepsLock) {
                val sums = LongArray(pumpkinSteps.maxOf { it.size }) { 0L }
                val counts = IntArray(sums.size) { 0 }
                for(turn in 0..<turns) {
                    val arr = pumpkinSteps[turn]
                    for(i in arr.indices) {
                        sums[i] += arr[i]
                        counts[i]++
                    }
                }
                return sums.mapIndexed { index, lng -> lng / counts[index].toDouble() } to counts.map { it / turns.toDouble() }
            }
        }
    }
    fun spawn(): SimMatrix {
        return SimMatrix(width,height, history).also {
            for(i in array.indices) {
                it.array[i] = when(val block = array[i]) {
                    is StemBlock -> Stem
                    is Pumpkin -> Dirt
                    else -> block
                }
            }
            it.updateAllState()
        }
    }
    fun copy() = SimMatrix(width,height,history).also { for(i in array.indices) it.array[i] = array[i] }
    fun addToHistory() {
        history.let {
            synchronized(it.pumpkinsLock) { it.totalPumpkins += totalPumpkins }
            synchronized(it.stepsLock) { it.totalSteps += totalSteps }
            synchronized(it.pumpkinStepsLock) { it.pumpkinSteps += pumpkinTotalSteps.toIntArray() }
            it.turns++
        }
    }
    val array = Array<Block>(width*height) { Dirt }
    var totalSteps = 0
        private set
    var totalPumpkins = 0
        private set
    var maxPumpkins = 0
        private set
    val pumpkinTotalSteps = mutableListOf<Int>()
    var lastHitI = -1
        private set
    var lastHitJ = -1
        private set
    var lastGrowDirection = -1
        private set
    private val stems = mutableListOf<Int>()
    fun updateAllState() {
        check()
        totalSteps = 0
        totalPumpkins = array.count { it is Pumpkin }
        pumpkinTotalSteps.clear()
        updateMaxPumpkins()
        stems.clear()
        stems.addAll(array.mapIndexedNotNull { index, block -> index.takeIf { block is Stem } })
    }
    fun updateMaxPumpkins() {
        pumpkinTotalSteps.addAll(List(totalPumpkins){0})
        maxPumpkins = maxPumpkins()
    }
    fun block(row:Int,col:Int) = array[row * width + col]
    fun setBlock(row:Int,col:Int,value: Block) { array[row * width + col] = value }
    fun inBound(i:Int,j:Int) = i in 0..<height && j in 0..<width
    private fun isPumpkin(i:Int,j:Int) = inBound(i,j) && block(i,j) is Pumpkin
    private fun isPumpkinGrowable(i:Int,j:Int) = inBound(i,j) && block(i,j).let { it is PumpkinGrowableBlock }
    fun maxPumpkins(): Int {
        val graph = mutableListOf<IntArray>()
        for(i in 0..<height) {
            for(j in 0..<width) {
                if(block(i,j) is Stem) {
                    val list = mutableListOf<Int>()
                    if(isPumpkinGrowable(i-1,j)) list += (i-1)*width+j
                    if(isPumpkinGrowable(i,j-1)) list += i*width+(j-1)
                    if(isPumpkinGrowable(i+1,j)) list += (i+1)*width+j
                    if(isPumpkinGrowable(i,j+1)) list += i*width+(j+1)
                    graph += list.toIntArray()
                }
            }
        }
        return Matcher(graph,array.size).maxMatching() + array.count { it is Pumpkin }
    }
    fun check() {
        val hasWater = BooleanArray(array.size) { false }
        for(i in 0..<height) {
            for(j in 0..<width) {
                val block = array[i * width + j]
                when(block) {
                    Water -> {
                        for(ii in max(0,i - 4)..min(height - 1,i + 4)) {
                            for(jj in max(0,j - 4)..min(width - 1,j + 4)) {
                                hasWater[ii*width + jj] = true
                            }
                        }
                    }
                    GrownStemL -> if(!isPumpkin(i,j-1)) array[i * width + j] = Stem
                    GrownStemT -> if(!isPumpkin(i-1,j)) array[i * width + j] = Stem
                    GrownStemR -> if(!isPumpkin(i,j+1)) array[i * width + j] = Stem
                    GrownStemB -> if(!isPumpkin(i+1,j)) array[i * width + j] = Stem
                    else -> {}
                }
            }
        }
        for(i in array.indices) {
            if(!hasWater[i]) {
                (array[i] as? NeedWaterBlock)?.let {
                    array[i] = it.replace
                }
            }
        }
    }
    private fun growPumpkin(i:Int,j:Int,block: GrownStemBlock) {
        var ii = i
        var jj = j
        when(block) {
            GrownStemL -> jj--
            GrownStemT -> ii--
            GrownStemR -> jj++
            GrownStemB -> ii++
        }
        if(!isPumpkinGrowable(ii,jj)) return
        setBlock(i,j,block)
        setBlock(ii,jj, Pumpkin)
        stems.remove(i*width+j)
        totalPumpkins++
        pumpkinTotalSteps += totalSteps
    }
    fun randomTick() {
        totalSteps++
        val i = Random.nextInt(0,height)
        val j = Random.nextInt(0,width)
        lastHitI = i
        lastHitJ = j
        lastGrowDirection = -1
        when(block(i,j)) {
            Stem -> if(Random.nextInt(1 + (25/growSpeed(i,j)).toInt()) == 0) {
                when(Random.nextInt(0,4).also { lastGrowDirection = it }) {
                    0 -> growPumpkin(i, j, GrownStemL)
                    1 -> growPumpkin(i, j, GrownStemT)
                    2 -> growPumpkin(i, j, GrownStemR)
                    3 -> growPumpkin(i, j, GrownStemB)
                }
            }
            else -> {}
        }
    }
    fun growable(): Boolean {
        return stems.isNotEmpty() && stems.find {
            val i = it/width
            val j = it%width
            isPumpkinGrowable(i,j-1) || isPumpkinGrowable(i-1,j) || isPumpkinGrowable(i,j+1) || isPumpkinGrowable(i+1,j)
        } != null
    }
    fun growAll() {
        while (growable()) {
            randomTick()
        }
    }
    private inline val Block?.acc get() = (this as? GrowAccelerateBlock)?.accelerateRate ?: 0.0
    fun growSpeed(i:Int,j:Int): Double {
        val lt = if(inBound(i-1,j-1)) block(i-1,j-1) else null
        val l = if(inBound(i,j-1)) block(i,j-1) else null
        val lb = if(inBound(i+1,j-1)) block(i+1,j-1) else null
        val t = if(inBound(i-1,j)) block(i-1,j) else null
        val c = if(inBound(i,j)) block(i,j) else null
        val b = if(inBound(i+1,j)) block(i+1,j) else null
        val rt = if(inBound(i-1,j+1)) block(i-1,j+1) else null
        val r = if(inBound(i,j+1)) block(i,j+1) else null
        val rb = if(inBound(i+1,j+1)) block(i+1,j+1) else null
        var result = lt.acc + l.acc + lb.acc + t.acc + c.acc/4 + b.acc + rt.acc + r.acc + rb.acc
        if(l is StemBlock || r is StemBlock)
            if(t is StemBlock || b is StemBlock)
                result /= 2
        if(lt is StemBlock || lb is StemBlock || rt is StemBlock || rb is StemBlock)
            result /= 2
        require(result >= 0)
        return result
    }

    context(ctx: DslContext)
    fun ui(tool: Block, editable: Boolean, maxSize: Measure = 200.scaled) {
        val w: Measure
        val h: Measure
        if(width > height) {
            w = maxSize
            h = maxSize * height / width
        } else {
            w = maxSize * width / height
            h = maxSize
        }
        Row(Modifier.size(w,h).padding(20.scaled)) {
            (0..<width).forEachWithId { j ->
                Column {
                    (0..<height).forEachWithId { i ->
                        val index = i * width + j
                        if(editable) array[index].image().clickable {
                            array[index] = tool
                            updateAllState()
                            history = History()
                        }.hoverMask() else array[index].image()
                    }
                }
            }
        }
    }
}