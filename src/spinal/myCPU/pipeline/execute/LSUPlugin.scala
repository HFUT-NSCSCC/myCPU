package myCPU.pipeline.execute

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.lib._
import myCPU.DataBundle

class LSUPlugin extends Plugin[Core]{
    val data = new DataBundle()
    // 设置数据端口的使能为寄存器，使其在默认状态下为False，避免译码信号未到达时候信号为未知，导致无法正常取指
    // val data_en = RegInit(False)
    val data_reg = Reg(new DataBundle())
    override def setup(pipeline: Core): Unit = {
        data.en := data_reg.en
        data.addr := data_reg.addr
        data.wdata := data_reg.wdata
        data.we := data_reg.we

    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        EXE1 plug new Area{
            import EXE1._

            val lsuSignals = input(exeSignals.lsuSignals)

            val src1 = lsuSignals.SRC1.asUInt //rj
            val imm = lsuSignals.IMM.asUInt // imm
            val vaddr = src1 + imm

            val memSignals = new MemSignals()
            memSignals.MEM_ADDR := vaddr.asBits & 0x7FFFFFFCL
            memSignals.MEM_EN := (lsuSignals.MEM_READ =/= B"0000" || lsuSignals.MEM_WRITE =/= B"0000") && arbitration.isValid
            memSignals.MEM_WE := lsuSignals.MEM_WRITE & (arbitration.isValid.asSInt.resize(4 bits).asBits)
            memSignals.MEM_WDATA := lsuSignals.SRC2
            memSignals.MEM_MASK := (lsuSignals.MEM_READ | lsuSignals.MEM_WRITE) |<< vaddr(1 downto 0)

            // 发起读写请求
            when(arbitration.notStuck){
                data_reg.en := memSignals.MEM_EN
                data_reg.addr := memSignals.MEM_ADDR
                data_reg.wdata := memSignals.MEM_WDATA
                data_reg.we := memSignals.MEM_WE
            }
            insert(exeSignals.memSignals) := memSignals
            // data.addr := memSignals.MEM_ADDR
            // // memory write
            // data.we := memSignals.MEM_WE
            // data.wdata := memSignals.MEM_WDATA
        }
        
        EXE2 plug new Area{
            import EXE2._
            val memSignals = input(exeSignals.memSignals)
            val lsuSignals = input(exeSignals.lsuSignals)
            // 暂停前端指令供应
            ISS.arbitration.haltItself setWhen((data_reg.en && !memSignals.MEM_ADDR(22) && arbitration.isValid) || (data.do_store_base))
            // 连续的写入需要等待上一个数据写入完成
            arbitration.haltItself setWhen(
                ((data.do_store_base || data.do_store_ext) && memSignals.MEM_MASK =/= B"0000" && arbitration.isValid))
            // memory read
            val rawData = data.rdata
            val rdata_ext = Bits(32 bits)
            switch(memSignals.MEM_MASK){
                is(B"0001"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(7 downto 0).asUInt.resize(32 bits).asBits, 
                                    rawData(7 downto 0).asSInt.resize(32 bits).asBits)
                }
                is(B"0010"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(15 downto 8).asUInt.resize(32 bits).asBits, 
                                    rawData(15 downto 8).asSInt.resize(32 bits).asBits)
                }
                is(B"0100"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(23 downto 16).asUInt.resize(32 bits).asBits, 
                                    rawData(23 downto 16).asSInt.resize(32 bits).asBits)
                }
                is(B"1000"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(31 downto 24).asUInt.resize(32 bits).asBits, 
                                    rawData(31 downto 24).asSInt.resize(32 bits).asBits)
                }
                is(B"0011"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(15 downto 0).asUInt.resize(32 bits).asBits, 
                                    rawData(15 downto 0).asSInt.resize(32 bits).asBits)
                }
                is(B"1100"){
                    rdata_ext := Mux(lsuSignals.MEM_READ_UE, 
                                    rawData(31 downto 16).asUInt.resize(32 bits).asBits, 
                                    rawData(31 downto 16).asSInt.resize(32 bits).asBits)
                }
                default{
                    rdata_ext := rawData
                }
            }
            insert(writeSignals.MEM_RDATA_WB) := rdata_ext
            
            
            
        }
    }
}
