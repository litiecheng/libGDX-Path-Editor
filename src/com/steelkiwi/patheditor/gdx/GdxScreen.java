package com.steelkiwi.patheditor.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.steelkiwi.patheditor.gdx.SplineBuilder.renderMode;
import com.steelkiwi.patheditor.widgets.GdxImage;

public class GdxScreen extends Screen implements IScreenStructureChangeListener {

	private int screenW, screenH;
	private Vector3 screenToStageCoords = new Vector3();
	
	private Camera camera;
	private Stage stage;
	private BGDrawer bgDrawer;
	private GdxImage bgImage;
	private SplineBuilder splineBuilder;
	private InputMultiplexer inputMultiplexer;
	
	public GdxScreen(GdxApp gdxApp, int stageW, int stageH, int canvasW, int canvasH) {
		super(gdxApp);
		this.screenW = stageW;
		this.screenH = stageH;
		
		camera = new Camera(canvasW, canvasH);
		camera.position.x = (int)(screenW / 2);
		camera.position.y = (int)(screenH / 2);
		
		stage = new Stage(stageW, stageH, false);
		stage.setCamera(camera);
		
		bgDrawer = new BGDrawer();
		
		inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new InputHandler());
		Gdx.input.setInputProcessor(inputMultiplexer);
	}

	@Override
	public void update(float deltaTime) {}

	@Override
	public void present(float deltaTime) {
		GL20 gl = Gdx.graphics.getGL20();
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		gl.glClearColor(0.698f, 0.698f, 0.698f, 1f);
		
		camera.update();
		bgDrawer.presentFakeBG(screenW, screenH, camera.combined);
		
		stage.act(deltaTime);
		stage.draw();
		
		bgDrawer.presentOverlayBG(screenW, screenH,
								 (int)camera.position.x, (int)camera.position.y,
								 (int)camera.viewportWidth, (int)camera.viewportHeight,
								 stage.getSpriteBatch());
		
		if (splineBuilder != null) { splineBuilder.present(camera.combined); }
	}

	@Override
	public void resize(int width, int height) {}
	
	@Override
	public void pause() {}

	@Override
	public void resume() {}

	@Override
	public void dispose() {
		bgImage = null;
		camera  = null;
		if (bgDrawer         != null) { bgDrawer.dispose();       bgDrawer = null;         }
		if (splineBuilder    != null) { splineBuilder.dispose();  splineBuilder = null;    }
		if (inputMultiplexer != null) { inputMultiplexer.clear(); inputMultiplexer = null; }
		if (stage            != null) { stage.dispose();          stage = null;            }
		screenToStageCoords = null;
		gdxApp = null;
	}
	
	// ==============================================================
	// create screen background
	// ==============================================================

	@Override
	public void onAddBGTexture(String name, String path, float scaleCoef) {
		if (bgImage != null) { bgImage.remove(); bgImage = null; }
		
		bgImage = new GdxImage(new Texture(Gdx.files.absolute(path)), name);
		bgImage.setTexPath(path);
		bgImage.scaleX = scaleCoef;
		bgImage.scaleY = scaleCoef;
		bgImage.x = (int)(screenW/2 - bgImage.width*bgImage.scaleX/2);
		bgImage.y = (int)(screenH/2 - bgImage.height*bgImage.scaleY/2);
		stage.addActor(bgImage);
	}
	
	@Override
	public GdxImage getBGImage() {
		return bgImage;
	}
	
	public void setBGImage(GdxImage bgImage) {
		this.bgImage = bgImage;
		if (bgImage != null) { stage.addActor(bgImage); }
	}
	
	// ==============================================================
	// create path
	// ==============================================================
	
	@Override
	public void onAddPath() {
		if (splineBuilder == null) {
			splineBuilder = new SplineBuilder();
		}
	}
	
	@Override
	public void onClearPath() {
		if (splineBuilder != null) {
			splineBuilder.clearSpline();
		}
	}

	@Override
	public renderMode getPathMode() {
		if (splineBuilder != null) {
			return splineBuilder.getPathMode();
		}
		return null;
	}
	
	@Override
	public void setPathMode(renderMode mode) {
		if (splineBuilder != null) {
			splineBuilder.setPathMode(mode);
		}
	}
	
	// ==============================================================
	// input handler
	// ==============================================================

	private class InputHandler extends InputAdapter {
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (pointer > 0) { return false; }
			camera.unproject(screenToStageCoords.set(screenX, screenY, 0));
			System.out.println(String.format("stage touch down at (%f, %f)", screenToStageCoords.x, screenToStageCoords.y));
			
			if ((button == 0) && (splineBuilder != null)) { //manage path
				return splineBuilder.touchDown(screenToStageCoords.x, screenToStageCoords.y);
			}
			if (button == 1) { //drag scene
				camera.setLastCamTouch(screenX, screenY);
				return true;
			}
			
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (pointer > 0) { return false; }
			camera.unproject(screenToStageCoords.set(screenX, screenY, 0));
			System.out.println(String.format("stage touch dragged at (%f, %f)", screenToStageCoords.x, screenToStageCoords.y));
			
			if (camera.isMapMoving()) {
				camera.move(screenX, screenY);
				return true;
			}
			else if (splineBuilder != null) {
				return splineBuilder.touchDragged(screenToStageCoords.x, screenToStageCoords.y);
			}
			
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (pointer > 0) { return false; }
			camera.unproject(screenToStageCoords.set(screenX, screenY, 0));
			System.out.println(String.format("stage touch up at (%f, %f)", screenToStageCoords.x, screenToStageCoords.y));
			
			if ((button == 1) || (camera.isMapMoving())) {
				camera.resetLastCamTouch();
			}
			return true;
		}

		@Override
		public boolean touchMoved(int screenX, int screenY) {
			camera.unproject(screenToStageCoords.set(screenX, screenY, 0));
			gdxApp.getUiHandler().updateMouseInfo(camera.zoom, screenToStageCoords.x, screenToStageCoords.y);
			return true;
		}
		
		@Override
		public boolean scrolled(int amount) {
			camera.zoom(amount);
			gdxApp.getUiHandler().updateMouseInfo(camera.zoom, screenToStageCoords.x, screenToStageCoords.y);
			return false;
		}
	}
}