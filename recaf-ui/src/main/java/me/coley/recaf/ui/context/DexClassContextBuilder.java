package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import static me.coley.recaf.ui.util.Menus.action;
import static me.coley.recaf.ui.util.Menus.createHeader;

/**
 * Context menu builder for android dex classes.
 *
 * @author Matt Coley
 */
public class DexClassContextBuilder extends DeclarableContextBuilder {
	private DexClassInfo info;

	/**
	 * @param info
	 * 		Class info.
	 *
	 * @return Builder.
	 */
	public DexClassContextBuilder setClassInfo(DexClassInfo info) {
		this.info = info;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = info.getName();
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(StringUtil.shortenPath(name), Icons.getClassIcon(info)));
		menu.getItems().add(action("menu.goto.class", Icons.OPEN, this::openDefinition));

		// TODO: Android dex class context menu items
		//  - copy
		//  - delete
		//  - refactor
		//    - move
		//    - rename
		//  - search
		//    - references to class
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = info.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForDexClass(name);
		if (resource == null)
			logger.warn("Could not find container resource for dex class {}", name);
		return resource;
	}

	@Override
	public DeclarableContextBuilder setDeclaration(boolean declaration) {
		return null;
	}

	@Override
	public void openDefinition() {
		CommonUX.openClass(info);
	}

	@Override
	public void rename() {
	}

	@Override
	public void delete() {
	}

	@Override
	public void copy() {
	}

	@Override
	public void search() {
	}
}
