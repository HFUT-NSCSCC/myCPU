package myCPU.pipeline.execute

import spinal.core._
import spinal.lib._
import myCPU.builder.Plugin
import myCPU.core.Core
import myCPU.constants.ALUOpType
import myCPU.pipeline.fetch.PCManagerPlugin
import myCPU.constants.OpSrc

class IntALUPlugin extends Plugin[Core]{
    override def setup(pipeline: Core): Unit = {

    }
  
    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._
            val aluSignals = input(exeSignals.intALUSignals)

            val IntALUOp = aluSignals.ALUOp

            val src1 = U(aluSignals.SRC1)
            val src2 = U(aluSignals.SRC2)

            val sa = src2(4 downto 0)
            val result = UInt(32 bits)
            switch(IntALUOp){
                import myCPU.constants.ALUOpType._
                is(ADD){
                    result := src1 + src2
                }
                is(SUB){
                    result := src1 - src2
                }
                is(XOR){
                    result := src1 ^ src2
                }
                is(AND){
                    result := src1 & src2
                }
                is(OR){
                    result := src1 | src2
                }
                is(NOR){
                    result := ~(src1 | src2)
                }
                is(SLT){
                    result := (src1.asSInt < src2.asSInt).asUInt.resize(32 bits)
                }
                is(SLTU){
                    result := (src1 < src2).asUInt.resize(32 bits)
                }
                is(SLL){
                    result := (src1 |<< sa)
                }
                is(SRL){
                    result := (src1 |>> sa)
                }
                is(SRA){
                    result := (src1.asSInt |>> sa).asUInt
                }
                is(LU12I){
                    result := src1
                }
                // default{
                //     result := 0
                // }
            }
        
            insert(writeSignals.ALU_RESULT_WB) := result.asBits
        }
    }
}
