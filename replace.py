import re

# 要替换的原始文本模式
pattern = r"""
initial\s+begin
\s+\$readmemb\("e:/coding/LA/2024837/thinpad_top\.srcs/sources_1/new/myCPU/MyCPU\.v_toplevel_defaultClockArea_cpu_RegFilePlugin_regFile\.bin",RegFilePlugin_regFile\);
\s+end
\s+assign\s+_zz_RegFilePlugin_regFile_port0\s+=\s+RegFilePlugin_regFile\[RegFilePlugin_src1Addr\];
\s+assign\s+_zz_RegFilePlugin_regFile_port1\s+=\s+RegFilePlugin_regFile\[RegFilePlugin_src2Addr\];
\s+always\s+@\s*\(posedge\s+io_clk\)\s+begin
\s+if\(_zz_1\)\s+begin
\s+RegFilePlugin_regFile\[WB_RegFilePlugin_regWritePort_payload_address\]\s+<=\s+WB_RegFilePlugin_regWritePort_payload_data;
\s+end
\s+end
"""

# 新文本
replacement_text = """
  assign _zz_RegFilePlugin_regFile_port0 = RegFilePlugin_regFile[RegFilePlugin_src1Addr];
  assign _zz_RegFilePlugin_regFile_port1 = RegFilePlugin_regFile[RegFilePlugin_src2Addr];
  integer i;
  always @(posedge io_clk) begin
    if (!io_reset) begin
      for(i = 0;i <32;i = i + 1) begin
        RegFilePlugin_regFile[i] <= 0;
      end
    end
    else if(_zz_1) begin
      RegFilePlugin_regFile[WB_RegFilePlugin_regWritePort_payload_address] <= WB_RegFilePlugin_regWritePort_payload_data;
    end
  end
"""

# 读取原始文件
with open('./build/gen/MyCPU.v', 'r') as file:
    file_contents = file.read()

# 替换文本
new_contents = re.sub(pattern, replacement_text, file_contents, flags=re.VERBOSE)

# 将新内容写回文件
with open('./build/gen/MyCPU.v', 'w') as file:
    file.write(new_contents)

print("替换完成。")