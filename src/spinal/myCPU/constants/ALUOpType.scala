package myCPU.constants
import spinal.core._
import spinal.lib._

object ALUOpType extends SpinalEnum(binarySequential){
    val ADD, SUB = newElement()
    val XOR, AND, OR, NOR = newElement()
    val SLT, SLTU = newElement()
    val SLL, SRL, SRA = newElement()
    val MUL = newElement()
    val LU12I = newElement()
}
