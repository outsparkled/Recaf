package me.coley.recaf.parse.assembly.visitors;

import me.coley.recaf.parse.assembly.*;
import me.coley.recaf.parse.assembly.parsers.OpParser;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.Function;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * Visitor that parses a body of instructions.
 *
 * @author Matt
 */
// TODO: Emitter interface that emits assembly from a methodnode
public class AssemblyVisitor implements Visitor {
	private static final Map<Integer, Function<AssemblyVisitor, Visitor>> visitors =
			new HashMap<>();
	private int line;
	private MethodNode method;
	private Variables variables = new Variables();
	private Labels labels = new Labels();
	// TODO: Labels
	// TODO: Try-catch ranges
	// TODO: Aliases
	//
	private boolean addVariables;
	// TODO: private boolean verify;
	// Method definition
	private int access;
	private String desc;

	/**
	 * Registers variables to <i>"this"</i> and method paramters.
	 *
	 * @param access
	 * 		Method access flag mask.
	 * @param desc
	 * 		Method descriptor.
	 */
	public void setupMethod(int access, String desc) {
		this.access = access;
		this.desc = desc;
		variables.setup(access, desc);
	}

	/**
	 * @param addVariables Flag to generate variable information in the generated method.
	 */
	public void setAddVariables(boolean addVariables) {
		this.addVariables = addVariables;
	}

	/**
	 * @param insn
	 * 		Instruction to append to the instructions list.
	 */
	public void appendInsn(AbstractInsnNode insn) {
		method.instructions.add(insn);
	}

	/**
	 * @return Method generated by {{@link #visit(String)}}.
	 */
	public MethodNode getMethod() {
		return method;
	}

	/**
	 * @return Method instructions generated by {{@link #visit(String)}}.
	 */
	public InsnList getInsnList() {
		return method.instructions;
	}

	/**
	 * @return Variable manager.
	 */
	public Variables getVariables() {
		return variables;
	}

	/**
	 * @return Label manager.
	 */
	public Labels getLabels() {
		return labels;
	}

	@Override
	public void visit(String text) throws LineParseException {
		// Iterate over lines in two passes:
		// - Debug collection
		// - Instruction parsing
		try {
			// reset
			labels.reset();
			variables.reset();
			if (desc != null)
				variables.setup(access, desc);
			// Setup method to fill
			method = new MethodNode();
			method.localVariables = new ArrayList<>();
			method.access = access;
			method.desc = desc;
			method.name = "assembled";
			// Preparse
			line = 1;
			String[] lines = StringUtil.splitNewline(text);
			for(String lineStr : lines) {
				getVisitor(lineStr).visitPre(lineStr);
				line++;
			}
			// TODO: When adding alias support, update strings in "lines"
			// Add instructions to method
			line = 1;
			for(String lineStr : lines) {
				getVisitor(lineStr).visit(lineStr);
				line++;
			}
			// Add variable information to method
			if (addVariables) {
				method.instructions.insert(labels.getStart());
				method.instructions.add(labels.getEnd());
				method.localVariables.addAll(variables.create(labels.getStart(), labels.getEnd()));
			}
		} catch(LineParseException ex) {
			// Some exceptions deeper in the visitor tree don't have access to the line.
			// So before we throw this, make sure it has the line.
			if (ex.getLine() == -1)
				ex.setLine(line);
			// Now throw it
			throw ex;
		}
	}

	/**
	 * @param lineStr
	 * 		Text to get suggestions for.
	 *
	 * @return List of suggestions for the last section of the chain parse list.
	 *
	 * @throws LineParseException
	 * 		When one of the parsers fails to interpret the line.
	 */
	public List<String> suggest(String lineStr) throws LineParseException {
		InstructionVisitor visitor;
		try {
			// Try to get visitor from instruction specified by the line.
			visitor = (InstructionVisitor) getVisitor(lineStr);
		} catch(LineParseException ex) {
			// Can't find an instruction? Suggest instructions!
			return new OpParser().suggest(lineStr);
		}
		// If the instruction is valid, use the instruction visitor's suggestions.
		try {
			// Fill the Parser chain values
			// - May throw 'LineParseException' since we're completing incompleted data.
			//   So this exception is expected and can be ignored here.
			visitor.parse(lineStr);
		} catch(LineParseException ex) {
			// Do nothing, we're expecting this
		}
		return visitor.suggest(lineStr);
	}

	/**
	 * @param text
	 * 		Text that has a leading instruction token.
	 *
	 * @return Visitor to parse the line.
	 *
	 * @throws LineParseException
	 * 		When the line does not have any identifier that links to an existing visitor
	 * 		implementation.
	 */
	private Visitor getVisitor(String text) throws LineParseException {
		return getVisitor(line, text);
	}

	/**
	 * @param text
	 * 		Text that has a leading instruction token.
	 * @param line
	 * 		The line the text resides on.
	 *
	 * @return Visitor to parse the line.
	 *
	 * @throws LineParseException
	 * 		When the line does not have any identifier that links to an existing visitor
	 * 		implementation.
	 */
	public Visitor getVisitor(int line, String text) throws LineParseException {
		String word = RegexUtil.getFirstWord(text);
		if(word != null) {
			String token = word.toUpperCase();
			if (OpcodeUtil.getInsnNames().contains(token)) {
				int opcode = OpcodeUtil.nameToOpcode(token);
				int type = OpcodeUtil.opcodeToType(opcode);
				if(visitors.containsKey(type))
					return visitors.get(type).apply(this);
			}
			throw new LineParseException(line, text, "Could not determine instruction: " + token);
		}
		throw new LineParseException(line, text, "Could not determine instruction. no content");
	}

	static {
		visitors.put(INSN, InsnVisitor::new);
		visitors.put(LINE, LineVisitor::new);
		visitors.put(LABEL, LabelVisitor::new);
		visitors.put(INT_INSN, IntVisitor::new);
		visitors.put(LDC_INSN, LdcVisitor::new);
		visitors.put(VAR_INSN, VarVisitor::new);
		visitors.put(IINC_INSN, IincVisitor::new);
		visitors.put(JUMP_INSN, JumpVisitor::new);
		visitors.put(TYPE_INSN, TypeVisitor::new);
		visitors.put(FIELD_INSN, FieldVisitor::new);
		visitors.put(METHOD_INSN, MethodVisitor::new);
		visitors.put(TABLESWITCH_INSN, TableSwitchVisitor::new);
		visitors.put(LOOKUPSWITCH_INSN, LookupSwitchVisitor::new);
		visitors.put(MULTIANEWARRAY_INSN, MultiANewArrayVisitor::new);
	}
}
