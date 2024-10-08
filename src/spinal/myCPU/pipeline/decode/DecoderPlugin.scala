package myCPU.pipeline.decode

import myCPU.builder.Plugin
import myCPU.core.Core
import spinal.core._
import spinal.core.internals.Literal
import spinal.lib._
import spinal.lib.logic.Masked
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.math.BigInt
import myCPU.builder.Stageable
import spinal.lib.logic.Symplify
import myCPU.constants._
import myCPU.core.LA32R


// 译码器, 参考自VexRiscv和NOP-Core
class DecoderPlugin extends Plugin[Core]{
    val encodings = mutable.LinkedHashMap[MaskedLiteral, ArrayBuffer[(Stageable[_ <: BaseType], BaseType)]]()
    val defaults = mutable.LinkedHashMap[Stageable[_ <: BaseType], BaseType]()
    
    def add(key: MaskedLiteral, values: Seq[(Stageable[_ <: BaseType], Any)]): Unit = {
        val instructionModel = encodings.getOrElseUpdate(key, ArrayBuffer[(Stageable[_ <: BaseType], BaseType)]())
        values.map{case (a, b) => {
            assert(!instructionModel.contains(a), s"Over specification of $a")
            val value = b match{
                case e: SpinalEnumElement[_] => e()
                case e: BaseType => e
            }
            instructionModel += (a -> value)
        }}
    }

    def add(encoding: Seq[(MaskedLiteral, Seq[(Stageable[_ <: BaseType], Any)])]): Unit = {
        encoding.foreach(e => this.add(e._1, e._2))
    }

    def addDefault(key: Stageable[_ <: BaseType], value: Any): Unit = {
        assert(!defaults.contains(key))
        defaults(key) = value match {
            case e: SpinalEnumElement[_] => e()
            case e: BaseType => e
        }
    }

    override def setup(pipeline: Core): Unit = {
        import pipeline._
        // import pipeline.config._
        import pipeline.decodeSignals._

        addDefault(FUType, FuType.ALU)
        addDefault(ALUOp, ALUOpType.ADD)
        addDefault(SRC1_FROM, OpSrc.NONE)
        addDefault(SRC2_FROM, OpSrc.NONE)
        addDefault(IMMExtType, ImmExtType.NONE)
        addDefault(JUMPType, JumpType.NONE)
        addDefault(MULOp, MULOpType.NONE)
        addDefault(BRUOp, BRUOpType.NONE)
        addDefault(MEM_WRITE, B"0000")
        addDefault(MEM_READ, B"0000")
        addDefault(MEM_READ_UE, False)
        addDefault(REG_WRITE_VALID, False)

        add(LA32R.NONE, List())

        val alu2RInst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.ALU,
            REG_WRITE_VALID -> True,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.REG
        )

        add(List(
            LA32R.ADD  -> (alu2RInst ++ List(ALUOp -> ALUOpType.ADD)),
            LA32R.SUB  -> (alu2RInst ++ List(ALUOp -> ALUOpType.SUB)),
            LA32R.SLT  -> (alu2RInst ++ List(ALUOp -> ALUOpType.SLT)),
            LA32R.SLTU -> (alu2RInst ++ List(ALUOp -> ALUOpType.SLTU)),
            LA32R.NOR  -> (alu2RInst ++ List(ALUOp -> ALUOpType.NOR)),
            LA32R.AND  -> (alu2RInst ++ List(ALUOp -> ALUOpType.AND)),
            LA32R.OR   -> (alu2RInst ++ List(ALUOp -> ALUOpType.OR)),
            LA32R.XOR  -> (alu2RInst ++ List(ALUOp -> ALUOpType.XOR)),
            LA32R.SLL  -> (alu2RInst ++ List(ALUOp -> ALUOpType.SLL)),
            LA32R.SRL  -> (alu2RInst ++ List(ALUOp -> ALUOpType.SRL)),
            LA32R.SRA  -> (alu2RInst ++ List(ALUOp -> ALUOpType.SRA)),
        )
        )

        val mul2RInst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.MUL,
            REG_WRITE_VALID -> True,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.REG
        )

        add(List(
            LA32R.MUL   -> (mul2RInst ++ List(MULOp -> MULOpType.MUL)),
            LA32R.MULH  -> (mul2RInst ++ List(MULOp -> MULOpType.MULH)),
            LA32R.DIV   -> (mul2RInst ++ List(MULOp -> MULOpType.DIV)),
            LA32R.DIVU  -> (mul2RInst ++ List(MULOp -> MULOpType.DIVU)),
            LA32R.MOD   -> (mul2RInst ++ List(MULOp -> MULOpType.MOD)),
            LA32R.MODU  -> (mul2RInst ++ List(MULOp -> MULOpType.MODU)),
        ))

        val alu2R1I8Inst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.ALU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.UI8,
            REG_WRITE_VALID -> True
        )

        add(List(
            LA32R.SLLI -> (alu2R1I8Inst ++ List(ALUOp -> ALUOpType.SLL)),
            LA32R.SRLI -> (alu2R1I8Inst ++ List(ALUOp -> ALUOpType.SRL)),
            LA32R.SRAI -> (alu2R1I8Inst ++ List(ALUOp -> ALUOpType.SRA)),
        ))

        val alu2R1I12SInst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.ALU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.SI12,
            REG_WRITE_VALID -> True
        )

        add(List(
            LA32R.ADDI  -> (alu2R1I12SInst ++ List(ALUOp -> ALUOpType.ADD)),
            LA32R.SLTI  -> (alu2R1I12SInst ++ List(ALUOp -> ALUOpType.SLT)),
            LA32R.SLTUI -> (alu2R1I12SInst ++ List(ALUOp -> ALUOpType.SLTU)),
        ))

        val alu2R1I12UInst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.ALU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.UI12,
            REG_WRITE_VALID -> True
        )

        add(List(
            LA32R.ANDI  -> (alu2R1I12UInst ++ List(ALUOp -> ALUOpType.AND)),
            LA32R.ORI   -> (alu2R1I12UInst ++ List(ALUOp -> ALUOpType.OR)),
            LA32R.XORI  -> (alu2R1I12UInst ++ List(ALUOp -> ALUOpType.XOR)),
        ))

        val lsu2R1I12InstLoad = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.LSU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.SI12,
        )

        add(List(
            LA32R.LDB -> (lsu2R1I12InstLoad ++ List(MEM_READ -> B"0001", MEM_READ_UE -> False, REG_WRITE_VALID -> True)),
            LA32R.LDH -> (lsu2R1I12InstLoad ++ List(MEM_READ -> B"0011", MEM_READ_UE -> False, REG_WRITE_VALID -> True)),
            LA32R.LDW -> (lsu2R1I12InstLoad ++ List(MEM_READ -> B"1111", MEM_READ_UE -> False, REG_WRITE_VALID -> True)),
            LA32R.LDBU -> (lsu2R1I12InstLoad ++ List(MEM_READ -> B"0001", MEM_READ_UE -> True, REG_WRITE_VALID -> True)),
            LA32R.LDHU -> (lsu2R1I12InstLoad ++ List(MEM_READ -> B"0011", MEM_READ_UE -> True, REG_WRITE_VALID -> True)),
        ))

        val lsu2R1I12InstStore = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.LSU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.REG,
            IMMExtType -> ImmExtType.SI12,
        )

        add(List(
            LA32R.STB -> (lsu2R1I12InstStore ++ List(MEM_WRITE -> B"0001")),
            LA32R.STH -> (lsu2R1I12InstStore ++ List(MEM_WRITE -> B"0011")),
            LA32R.STW -> (lsu2R1I12InstStore ++ List(MEM_WRITE -> B"1111")),
        ))

        val bru2R1I21Inst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.BRU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.REG,
            IMMExtType -> ImmExtType.SI16,
        )

        add(List(
            LA32R.JIRL -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.JIRL, REG_WRITE_VALID -> True)),
        ))

        add(List(
            LA32R.BEQ  -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.EQ)),
            LA32R.BNE  -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.NEQ)),
            LA32R.BLT  -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.LT)),
            LA32R.BLTU -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.LTU)),
            LA32R.BGE  -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.GE)),
            LA32R.BGEU -> (bru2R1I21Inst ++ List(JUMPType -> JumpType.Branch, BRUOp -> BRUOpType.GEU)),
        ))

        val alu1R1I20Inst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.ALU,
            SRC1_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.SI20,
            ALUOp -> ALUOpType.LU12I,
            REG_WRITE_VALID -> True
        )

        add(List(
            LA32R.LU12I -> (alu1R1I20Inst ++ List()),
        ))

        val bru0R1IInst = List[(Stageable[_ <: BaseType], Any)](
            FUType -> FuType.BRU,
            SRC1_FROM -> OpSrc.REG,
            SRC2_FROM -> OpSrc.REG,
            IMMExtType -> ImmExtType.SI26,
        )

        add(List(
            LA32R.LA_B -> (bru0R1IInst ++ List(JUMPType -> JumpType.JB)),
            LA32R.BL -> (bru0R1IInst ++ List(JUMPType -> JumpType.JBL, REG_WRITE_VALID -> True)),
        ))

        add(LA32R.PCADDU12I, List(
            FUType -> FuType.ALU, 
            SRC1_FROM -> OpSrc.PC,
            SRC2_FROM -> OpSrc.IMM,
            IMMExtType -> ImmExtType.SI20, 
            REG_WRITE_VALID -> True))
    }

    def build(pipeline: Core): Unit = {
        import pipeline._
        import pipeline.config._

        ID plug new Area{
            import pipeline.ID._
            val immExt = ImmExt()
            immExt.io.inst <> input(fetchSignals.INST)
            immExt.io.immType <> output(decodeSignals.IMMExtType)
            insert(decodeSignals.IMM) := immExt.io.imm
        }

        ID plug new Area{
            import pipeline.ID._

            val stageables = (encodings.flatMap(_._2.map(_._1)) ++ defaults.map(_._1)).toList.distinct
            var offset = 0
            var defaultValue, defaultCare = BigInt(0)
            val offsetOf = mutable.LinkedHashMap[Stageable[_ <: BaseType], Int]()
    
            stageables.foreach(e => {
                defaults.get(e) match {
                    case Some(value) => {
                        value.head.source match {
                            case literal: EnumLiteral[_] => literal.fixEncoding(e.dataType.asInstanceOf[SpinalEnumCraft[_]].getEncoding)
                            case _ => 
                        }
                        defaultValue += value.head.source.asInstanceOf[Literal].getValue << offset
                        defaultCare += ((BigInt(1) << e.dataType.getBitsWidth) - 1) << offset
                    }
                    case _ => 
                }
                offsetOf(e) = offset
                offset += e.dataType.getBitsWidth
                println(e.dataType + ": " + offsetOf(e) + ":" + (offset - 1))
            })
    
            val spec = encodings.map{case(key, values) => 
                var decodedValue = defaultValue    
                var decodedCare = defaultCare
                for ((e, literal) <- values) {
                    literal.head.source match {
                        case literal: EnumLiteral[_] => literal.fixEncoding(e.dataType.asInstanceOf[SpinalEnumCraft[_]].getEncoding)
                        case _ =>
                    }
                    val offset = offsetOf(e)
                    decodedValue |= literal.head.source.asInstanceOf[Literal].getValue << offset
                    decodedCare |= ((BigInt(1) << e.dataType.getBitsWidth) - 1) << offset
                }
                (Masked(key.value, key.careAbout), Masked(decodedValue, decodedCare))
            }

            val decodedBits = Bits(stageables.foldLeft(0)(_ + _.dataType.getBitsWidth) bits)
            decodedBits := Symplify(input(fetchSignals.INST), spec, decodedBits.getWidth)
    
            offset = 0
            stageables.foreach(e => {
                insert(e).assignFromBits(decodedBits(offset, e.dataType.getBitsWidth bits))
                offset += e.dataType.getBitsWidth
            })
        }
    }
        
}
