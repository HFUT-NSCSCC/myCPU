package myCPU.core

import myCPU.builder.Plugin
import spinal.core._
import spinal.lib._
import myCPU._
import myCPU.constants._
import myCPU.core.LA32R._
import _root_.myCPU.constants.JumpType._
import _root_.myCPU.constants.FuType._
import myCPU.constants.JumpType._

class RegFilePlugin extends Plugin[Core]{
    // val debug = new Bundle{
    //     val wen = Bool
    //     val wnum = Bits(5 bits)
    //     val wdata = Bits(32 bits)
        
    // }
    // val debug = out(new DebugBundle())

    val wvalid = Bool
    val waddr = UInt(RegAddrWidth bits)
    val wdata = Bits(DataWidth bits)

    val fromEXE1 = Bits(DataWidth bits)
    val fromEXE2 = Bits(DataWidth bits)
    val fromEXE3 = Bits(DataWidth bits)
    val fromWB = Bits(DataWidth bits)

    // val src1Addr = UInt(RegAddrWidth bits)
    // val src2Addr = UInt(RegAddrWidth bits)

    val rs1Forwardable = Bool
    val rs1ForwardType = Bits(ForwardType().getBitsWidth bits)
    val rs2Forwardable = Bool
    val rs2ForwardType = Bits(ForwardType().getBitsWidth bits)
    override def setup(pipeline: Core): Unit = {

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        val global = pipeline plug new Area{
            val regFile = Mem(Bits(32 bits), NR_REG).addAttribute(Verilator.public)
            regFile.init(List.fill(NR_REG)(B(0, 32 bits)))

            switch(EXE1.output(writeSignals.FUType_WB)) {
                is(FuType.ALU) {
                    fromEXE1 := EXE1.output(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.BRU) {
                    fromEXE1 := (EXE1.output(fetchSignals.PC) + U(4)).asBits
                }
                default{
                    fromEXE1 := 0
                }
            }

            switch(EXE2.output(writeSignals.FUType_WB)) {
                is(FuType.ALU) {
                    fromEXE2 := EXE2.output(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.BRU) {
                    fromEXE2 := (EXE2.output(fetchSignals.PC) + U(4)).asBits
                }
                // is(FuType.LSU) {
                //     fromEXE2 := EXE2.output(writeSignals.MEM_RDATA_WB)
                // }
                is(FuType.MUL) {
                    fromEXE2 := EXE2.output(writeSignals.MUL_RESULT_WB)
                }
                default{
                    fromEXE2 := 0
                }
            }
            switch(EXE3.output(writeSignals.FUType_WB)) {
                is(FuType.ALU) {
                    fromEXE3 := EXE3.output(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.BRU) {
                    fromEXE3 := (EXE3.output(fetchSignals.PC) + U(4)).asBits
                }
                is(FuType.LSU) {
                    fromEXE3 := EXE3.output(writeSignals.MEM_RDATA_WB)
                }
                is(FuType.MUL) {
                    fromEXE3 := EXE3.output(writeSignals.MUL_RESULT_WB)
                }
            }
            fromWB := wdata

            val rs1BypassNetwork = new BypassNetwork()
            val rs2BypassNetwork = new BypassNetwork()
            // rs1BypassNetwork.io.rsISS <> src1Addr
            rs1BypassNetwork.io.rdEXE1 <> EXE1.input(writeSignals.REG_WRITE_ADDR_WB)
            rs1BypassNetwork.io.rdEXE2 <> EXE2.input(writeSignals.REG_WRITE_ADDR_WB)
            rs1BypassNetwork.io.rdEXE3 <> EXE3.input(writeSignals.REG_WRITE_ADDR_WB)
            rs1BypassNetwork.io.rdWB   <> waddr
            rs1BypassNetwork.io.fuTypeEXE1 <> EXE1.input(writeSignals.FUType_WB)
            rs1BypassNetwork.io.fuTypeEXE2 <> EXE2.input(writeSignals.FUType_WB)
            rs1BypassNetwork.io.fuTypeEXE3 <> EXE3.input(writeSignals.FUType_WB)
            rs1BypassNetwork.io.fuTypeWB <> WB.input(writeSignals.FUType_WB)
            rs1BypassNetwork.io.regWriteValidEXE1 := EXE1.input(writeSignals.REG_WRITE_VALID_WB) & EXE1.arbitration.isValidNotStuck
            rs1BypassNetwork.io.regWriteValidEXE2 := EXE2.input(writeSignals.REG_WRITE_VALID_WB) & EXE2.arbitration.isValidNotStuck
            rs1BypassNetwork.io.regWriteValidEXE3 := EXE3.input(writeSignals.REG_WRITE_VALID_WB) & EXE3.arbitration.isValidNotStuck
            rs1BypassNetwork.io.regWriteValidWB   <> wvalid
            rs1BypassNetwork.io.rsForwardType     <> rs1ForwardType
            rs1BypassNetwork.io.forwardable       <> rs1Forwardable

            // rs2BypassNetwork.io.rsISS <> src2Addr
            rs2BypassNetwork.io.rdEXE1 <> EXE1.input(writeSignals.REG_WRITE_ADDR_WB)
            rs2BypassNetwork.io.rdEXE2 <> EXE2.input(writeSignals.REG_WRITE_ADDR_WB)
            rs2BypassNetwork.io.rdEXE3 <> EXE3.input(writeSignals.REG_WRITE_ADDR_WB)
            rs2BypassNetwork.io.rdWB   <> waddr
            rs2BypassNetwork.io.fuTypeEXE1 <> EXE1.input(writeSignals.FUType_WB)
            rs2BypassNetwork.io.fuTypeEXE2 <> EXE2.input(writeSignals.FUType_WB)
            rs2BypassNetwork.io.fuTypeEXE3 <> EXE3.input(writeSignals.FUType_WB)
            rs2BypassNetwork.io.fuTypeWB <> WB.input(writeSignals.FUType_WB)
            rs2BypassNetwork.io.regWriteValidEXE1 := EXE1.input(writeSignals.REG_WRITE_VALID_WB) & EXE1.arbitration.isValidNotStuck
            rs2BypassNetwork.io.regWriteValidEXE2 := EXE2.input(writeSignals.REG_WRITE_VALID_WB) & EXE2.arbitration.isValidNotStuck
            rs2BypassNetwork.io.regWriteValidEXE3 := EXE3.input(writeSignals.REG_WRITE_VALID_WB) & EXE3.arbitration.isValidNotStuck
            rs2BypassNetwork.io.regWriteValidWB   <> wvalid
            rs2BypassNetwork.io.rsForwardType     <> rs2ForwardType
            rs2BypassNetwork.io.forwardable       <> rs2Forwardable

        }

        ID plug new Area{
            import ID._

            val inst = input(fetchSignals.INST)
            val fuType = output(decodeSignals.FUType)
            val src1Addr = U(inst(LA32R.rjRange))
            val src2Addr = (fuType === FuType.ALU || fuType === FuType.MUL) ? U(inst(LA32R.rkRange)) | U(inst(LA32R.rdRange))
            insert(decodeSignals.SRC1Addr) := src1Addr
            insert(decodeSignals.SRC2Addr) := src2Addr
            insert(decodeSignals.REG_WRITE_ADDR) := (output(decodeSignals.JUMPType) =/= JumpType.JBL) ? inst(LA32R.rdRange).asUInt | U(1)
        }

        ISS plug new Area{
            import ISS._

            val inst = input(fetchSignals.INST)
            val fuType = input(decodeSignals.FUType)
            val src1Addr = input(decodeSignals.SRC1Addr)
            val src2Addr = input(decodeSignals.SRC2Addr)
            global.rs1BypassNetwork.io.rsISS <> src1Addr
            global.rs2BypassNetwork.io.rsISS <> src2Addr

            // val src1Data = (wvalid && src1Addr.asBits === waddr && (input(decodeSignals.SRC1_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src1Addr)
            // val src2Data = (wvalid && src2Addr.asBits === waddr && (input(decodeSignals.SRC2_FROM) === ALUOpSrc.REG)) ? (wdata) | global.regFile.readAsync(src2Addr)
            val src1Data = Bits(32 bits)
            val src2Data = Bits(32 bits)
            switch(rs1ForwardType){
                is(ForwardType.FROMEXE1.asBits) {
                    src1Data := fromEXE1
                }
                is(ForwardType.FROMEXE2.asBits) {
                    src1Data := fromEXE2
                }
                is(ForwardType.FROMEXE3.asBits) {
                    src1Data := fromEXE3
                }
                is(ForwardType.FROMWB.asBits) {
                    src1Data := wdata
                }
                default {
                    src1Data := global.regFile.readAsync(src1Addr)
                }
            }

            switch(rs2ForwardType){
                is(ForwardType.FROMEXE1.asBits) {
                    src2Data := fromEXE1
                }
                is(ForwardType.FROMEXE2.asBits) {
                    src2Data := fromEXE2
                }
                is(ForwardType.FROMEXE3.asBits) {
                    src2Data := fromEXE3
                }
                is(ForwardType.FROMWB.asBits) {
                    src2Data := wdata
                }
                default {
                    src2Data := global.regFile.readAsync(src2Addr)
                }
            }
            
            // insert(decodeSignals.SRC1Addr) := src1Addr
            // insert(decodeSignals.SRC2Addr) := src2Addr
            insert(decodeSignals.SRC1) := (src1Addr === 0) ? B(0, 32 bits) | src1Data
            insert(decodeSignals.SRC2) := (src2Addr === 0) ? B(0, 32 bits) | src2Data
            // insert(decodeSignals.REG_WRITE_ADDR) := (input(decodeSignals.JUMPType) =/= JumpType.JBL) ? inst(LA32R.rdRange).asUInt | U(1)
            
        }

        WB plug new Area{
            import WB._

            val regWritePort = global.regFile.writePort()

            wvalid := input(decodeSignals.REG_WRITE_VALID) && arbitration.isValidNotStuck
            waddr := input(writeSignals.REG_WRITE_ADDR_WB)
            // val aluResult = input(RESULT)
            // val memRdata = input(MEM_RDATA)
            // val data = input(REG_WRITE_DATA)
            // val data = Select{
            //     (input(FUType) === ALU) -> input(RESULT);
            //     ((input(FUType) === LSU) && (input(MEM_READ) =/= B"0000")) -> input(MEM_RDATA);
            //     ((input(FUType) === BRU) && ((input(JUMPType) === JBL) || (input(JUMPType) === JB))) -> (input(PC).asUInt + U(4)).asBits;
            //     default -> B"32'h00000000"
            // }

            switch(input(writeSignals.FUType_WB)){
                is(FuType.ALU){
                    wdata := input(writeSignals.ALU_RESULT_WB)
                }
                is(FuType.LSU){
                    wdata := input(writeSignals.MEM_RDATA_WB)
                }
                is(FuType.BRU){
                    wdata := (input(fetchSignals.PC) + U(4)).asBits
                }
                is(FuType.MUL){
                    wdata := input(writeSignals.MUL_RESULT_WB)
                }
            }

            // debug.pc := input(fetchSignals.PC)
            // debug.we := wvalid
            // debug.wnum := waddr
            // debug.wdata := wdata

            regWritePort.valid := wvalid
            regWritePort.address := waddr
            regWritePort.data := wdata
        }
    }
  
}
