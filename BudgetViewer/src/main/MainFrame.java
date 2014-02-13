package main;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;


public class MainFrame extends JFrame {

	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
	
	private ArrayList<String[]> fileContent = new ArrayList<String[]>();
	private double totalEarnings;
	private double totalSpendings;
	private Color colorUnused; private String colorUnusedStr;
	private ArrayList<Category> categories = new ArrayList<Category>();
	private ArrayList<SpecCategory> specCategories = new ArrayList<SpecCategory>();
	private int projTimeAmount;
	private String projUnitOfTimeStr;
	private double forecastEarningsAmt;
	private int forecastEarningsTimeAmount;
	private UnitOfTime forecastEarningsUnitOfTime; private String forecastEarningsUnitOfTimeStr;
	private double forecastEarningsCalcAmt = 0;
	private double forecastSpendingsCalcAmt;
	private ArrayList<ProjectionSetting> projectionSettings = new ArrayList<ProjectionSetting>();
	private HashMap<Category, ProjectionSetting> catProjMap = new HashMap<Category, ProjectionSetting>();
	private String factorInStr;

	private ViewOption viewOption = null;

	private Box chartAndOptions = new Box(BoxLayout.Y_AXIS);
	private JPanel chart = new JPanel() {
		@Override
		public void paintComponent(Graphics g) {
			if (viewOption == ViewOption.FORECAST && forecastEarningsCalcAmt == 0) {
				g.drawString("Your forecast yields no earnings", 128, 150);
			} else if (viewOption == ViewOption.FORECAST && roundCent(forecastEarningsCalcAmt - forecastSpendingsCalcAmt) < 0) {
				g.drawString("You can't afford this budget", 128, 150);
			} else if (viewOption == ViewOption.COMPLETE_BUDGET && totalEarnings == 0) {
				g.drawString("You have no earnings", 128, 150);
			} else if (viewOption == ViewOption.UNUSED_MONEY && roundCent(totalEarnings - totalSpendings) == 0) {
				g.drawString("You have no unused money", 128, 150);
			} else {
				if (viewOption == ViewOption.COMPLETE_BUDGET) {
					int angle = 90;
					for (Category cat: categories) {
						g.setColor(cat.color);
						g.fillArc(20, 20, 256, 256, (angle = angle - (int)Math.round(360 * (cat.spending / totalEarnings))), (int)Math.round(360 * (cat.spending / totalEarnings)));
					}
					g.setColor(colorUnused);
					g.fillArc(20, 20, 256, 256, angle - (int)Math.round(360 * ((totalEarnings - totalSpendings) / totalEarnings)), (int)Math.round(360 * ((totalEarnings - totalSpendings) / totalEarnings)));
					for (SpecCategory spec: specCategories) {
						if (spec.showInPie) {
							configPaint(g, spec.color);
							g.fillArc(20, 20, 256, 256, (angle = angle - (int)Math.round(360 * (spec.spending / totalEarnings))), (int)Math.round(360 * (spec.spending / totalEarnings)));
						}
					}
				} else if (viewOption == ViewOption.UNUSED_MONEY) {
					g.setColor(colorUnused);
					g.fillArc(20, 20, 256, 256, 0, 360);
					int angle = 90;
					for (SpecCategory spec: specCategories) {
						if (spec.showInPie) {
							configPaint(g, spec.color);
							g.fillArc(20, 20, 256, 256, (angle = angle - (int)Math.round(360 * (spec.spending / (totalEarnings - totalSpendings)))), (int)Math.round(360 * (spec.spending / (totalEarnings - totalSpendings))));
						}
					}
				} else {
					int angle = 90;
					for (ProjectionSetting sett: projectionSettings) {
						g.setColor(sett.category.color);
						g.fillArc(20, 20, 256, 256, (angle = angle - (int)Math.round(360 * ((sett.calcAmt + (Boolean.parseBoolean(factorInStr) ? sett.category.spending : 0)) / forecastEarningsCalcAmt))), (int)Math.round(360 * ((sett.calcAmt + (Boolean.parseBoolean(factorInStr) ? sett.category.spending : 0)) / forecastEarningsCalcAmt)));
					}
					g.setColor(colorUnused);
					g.fillArc(20, 20, 256, 256, angle - (int)Math.round(360 * ((forecastEarningsCalcAmt - forecastSpendingsCalcAmt) / forecastEarningsCalcAmt)), (int)Math.round(360 * ((forecastEarningsCalcAmt - forecastSpendingsCalcAmt) / forecastEarningsCalcAmt)));
					for (SpecCategory spec: specCategories) {
						if (spec.showInPie) {
							configPaint(g, spec.color);
							g.fillArc(20, 20, 256, 256, (angle = angle - (int)Math.round(360 * (spec.spending / forecastEarningsCalcAmt))), (int)Math.round(360 * (spec.spending / forecastEarningsCalcAmt)));
						}
					}
				}
			}
		}

		private void configPaint(Graphics g, Color color) {
			BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
			Graphics g2 = img.createGraphics();
			g2.setColor(colorUnused);
			g2.drawRect(0, 0, 4, 4);
			g2.setColor(color);
			g2.drawLine(0, 2, 2, 0);
			g2.drawLine(0, 3, 3, 0);
			g2.drawLine(3, 3, 3, 3);
			Rectangle2D rect = new Rectangle2D.Double(0, 0, 4, 4);
			((Graphics2D)g).setPaint(new TexturePaint(img, rect)); 
		}
	};
	private JPanel options = new JPanel();
	private JPanel inputs = new JPanel(new BorderLayout());

	public static void main(String[] args) {
		new MainFrame().start();
	}

	private JRadioButtonMenuItem viewCurrent;
	private JRadioButtonMenuItem viewUnused;
	private JRadioButtonMenuItem viewProjection;
	private void start() {
		try {
			final File file = new File("budget.txt");
			setPreferredSize(new Dimension(550, 423));
			setTitle("Budget Viewer");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				fileContent.add(line.split(":"));
			}
			reader.close();
			readBudgetSettings();
			updateFile();
			JMenuBar menuBar = new JMenuBar();
			ButtonGroup menuBarGroup = new ButtonGroup();
			viewCurrent = new JRadioButtonMenuItem("Complete budget");
			menuBar.add(viewCurrent);
			menuBarGroup.add(viewCurrent);
			viewUnused = new JRadioButtonMenuItem("Unused money");
			menuBar.add(viewUnused);
			menuBarGroup.add(viewUnused);
			viewProjection = new JRadioButtonMenuItem("Forecast");
			menuBar.add(viewProjection);
			menuBarGroup.add(viewProjection);
			JMenu about = new JMenu("About");
			menuBar.add(about);
			JMenuItem credits = new JMenuItem("Credits");
			about.add(credits);
			JMenuItem openFile = new JMenuItem("View records");
			about.add(openFile);
			setJMenuBar(menuBar);

			initializeButtons();
			initializeMessageDialogs();
			refreshCategoriesDisplay();

			chartAndOptions.setPreferredSize(new Dimension(350, 400));
			chart.setPreferredSize(new Dimension(350, 350));
			chartAndOptions.add(chart, BorderLayout.NORTH);
			options.setPreferredSize(new Dimension(350, 50));
			inputs.setPreferredSize(new Dimension(190, 400));
			add(chartAndOptions, BorderLayout.CENTER);
			chartAndOptions.add(chart, BorderLayout.CENTER);
			chartAndOptions.add(options, BorderLayout.SOUTH);
			add(inputs, BorderLayout.EAST);
			options.add(earn);
			options.add(spend);
			options.add(newCat);
			options.add(newSpec);

			viewCurrent.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (viewOption == ViewOption.COMPLETE_BUDGET) {
						return;
					}
					try {
						makeScreen(ViewOption.COMPLETE_BUDGET);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			viewUnused.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (viewOption == ViewOption.UNUSED_MONEY) {
						return;
					}
					try {
						makeScreen(ViewOption.UNUSED_MONEY);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			viewProjection.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (viewOption == ViewOption.FORECAST) {
						return;
					}
					try {
						makeScreen(ViewOption.FORECAST);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(MainFrame.this, e.getClass().toString() + ": " + e.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			credits.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JOptionPane.showMessageDialog(MainFrame.this, "Made by kurantoB");
				}
			});
			openFile.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Desktop dt = Desktop.getDesktop();
					try {
						dt.open(file);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "IOException", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			viewCurrent.doClick();

			setLocationByPlatform(true);
			pack();
			setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	private Box spendPanel = new Box(BoxLayout.Y_AXIS);
	private JComboBox selectCatCombo = new JComboBox();
	private Box newCatBox = new Box(BoxLayout.X_AXIS);
	private Box newCat1 = new Box(BoxLayout.Y_AXIS);
	private JPanel catNameCont = new JPanel();
	private JTextField catName = new JTextField(11);
	private Box colorBigBox = new Box(BoxLayout.Y_AXIS);
	private Box colorBigBoxTop = new Box(BoxLayout.X_AXIS);
	private Box color2 = new Box(BoxLayout.Y_AXIS);
	private JComboBox colorCombo = new JComboBox(new String[]{"black", "blue", "cyan", "dark gray", "gray", "green", "light gray", "magenta", "orange", "pink", "red", "white", "yellow"});
	private JPanel color3 = new JPanel();
	private Box color4 = new Box(BoxLayout.Y_AXIS);
	private Box colorTextFields = new Box(BoxLayout.X_AXIS);
	private JTextField redFld = new JTextField(3);
	private JTextField greenFld = new JTextField(3);
	private JTextField blueFld = new JTextField(3);
	private JPanel colorBigBoxBottom = new JPanel();
	private JButton colorPreview = new JButton("Preview");
	private Box newSpecBox = new Box(BoxLayout.Y_AXIS);
	private Box newSpecBoxTop = new Box(BoxLayout.X_AXIS);
	private Box newSpecName = new Box(BoxLayout.Y_AXIS);
	private JPanel newSpecNameFldCont = new JPanel();
	private JTextField newSpecNameFld = new JTextField(11);
	private JCheckBox newSpecShow = new JCheckBox("Show in pie", true);
	private Box editBox = new Box(BoxLayout.X_AXIS);
	private Box editNameBox = new Box(BoxLayout.Y_AXIS);
	private JLabel editNameLbl;
	private JPanel editNameCont = new JPanel();
	private JTextField editName = new JTextField(11);
	private Box editSpecRight = new Box(BoxLayout.Y_AXIS);
	private Box editSpecAmtBox = new Box(BoxLayout.Y_AXIS);
	private JPanel editSpecAmtCont = new JPanel();
	private JTextField editSpecAmt = new JTextField(11);
	private JCheckBox editSpecShow = new JCheckBox("Show in pie");
	private JPanel projSettingsPromptPnl = new JPanel(new BorderLayout());
	private Box projSettingsPromptBox = new Box(BoxLayout.Y_AXIS);
	private Box projFastForward = new Box(BoxLayout.X_AXIS);
	private JTextField projFFTimeAmt = new JTextField(4);
	private JComboBox projFFUOT = new JComboBox(new String[]{"day", "week", "month", "year"});
	private Box projEarningsSett = new Box(BoxLayout.X_AXIS);
	private JTextField projEarningsAmt = new JTextField(9);
	private JTextField projEarningsTimeAmt = new JTextField(4);
	private JComboBox projEarningsUOT = new JComboBox(new String[]{"day", "week", "month", "year"});
	private HashMap<ProjectionSetting, JTextField> projCatsMapAmt = new HashMap<ProjectionSetting, JTextField>();
	private HashMap<ProjectionSetting, JTextField> projCatsMapTimeAmt = new HashMap<ProjectionSetting, JTextField>();
	private HashMap<ProjectionSetting, JComboBox> projCatsMapUnitOfTime = new HashMap<ProjectionSetting, JComboBox>();
	private Box projDynamic = new Box(BoxLayout.Y_AXIS);
	private JCheckBox projFactorIn = new JCheckBox("Factor in current earnings/spendings");
	private Box editUnusedColorBox = new Box(BoxLayout.Y_AXIS);
	private FocusListener colorFldFocusListen = new FocusListener() {
		@Override
		public void focusGained(FocusEvent e) {
			colorCombo.setEnabled(false);
		}
		@Override
		public void focusLost(FocusEvent e) {
			if (redFld.getText().length() == 0 && greenFld.getText().length() == 0 && blueFld.getText().length() == 0) {
				colorCombo.setEnabled(true);
			}
		}
	};
	private ActionListener colorPreviewListener = new ActionListener() {
		private int redInt;
		private int greenInt;
		private int blueInt;
		private Color theColor;
		private JPanel previewPanel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				g.setColor(theColor);
				g.fillRect(57, 0, 32, 32);
			}
		};
		@Override
		public void actionPerformed(ActionEvent e) {
			if (colorCombo.isEnabled()) {
				try {
					theColor = getColor((String)colorCombo.getSelectedItem());
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} else {
				try {
					redInt = Integer.parseInt(redFld.getText());
					greenInt = Integer.parseInt(greenFld.getText());
					blueInt = Integer.parseInt(blueFld.getText());
					theColor = new Color(redInt, greenInt, blueInt);
				} catch (Exception e1) {
					if (e1 instanceof NumberFormatException || e1 instanceof IllegalArgumentException) {
						JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid input for color", "Number Format Exception", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
			}
			JOptionPane.showMessageDialog(MainFrame.this, previewPanel);
		}
	};

	private void initializeMessageDialogs() {
		JLabel spendLbl = new JLabel("Select category and enter amount:");
		spendPanel.add(spendLbl);
		spendLbl.setAlignmentX(CENTER_ALIGNMENT);
		spendPanel.add(Box.createVerticalStrut(10));
		spendPanel.add(selectCatCombo);

		colorBigBox.add(colorBigBoxTop);
		colorBigBoxTop.add(color2);
		color2.add(new JLabel("Choose a color:"));
		color2.add(colorCombo);
		colorBigBoxTop.add(color3);
		color3.add(new JLabel(" -OR- "));
		colorBigBoxTop.add(color4);
		color4.add(new JLabel("Enter integer values for:"));
		color4.add(colorTextFields);
		colorTextFields.add(new JLabel("red:"));
		colorTextFields.add(redFld);
		colorTextFields.add(new JLabel("green:"));
		colorTextFields.add(greenFld);
		colorTextFields.add(new JLabel("blue:"));
		colorTextFields.add(blueFld);
		colorBigBox.add(colorBigBoxBottom);
		colorBigBoxBottom.add(colorPreview);
		redFld.addFocusListener(colorFldFocusListen);
		greenFld.addFocusListener(colorFldFocusListen);
		blueFld.addFocusListener(colorFldFocusListen);
		colorPreview.addActionListener(colorPreviewListener);

		newCatBox.add(newCat1);
		JLabel newCatNameLbl = new JLabel("Name of category:");
		newCat1.add(newCatNameLbl);
		newCatNameLbl.setAlignmentX(CENTER_ALIGNMENT);
		catNameCont.add(catName);
		newCat1.add(catNameCont);

		newSpecBox.add(newSpecBoxTop);
		newSpecBoxTop.add(newSpecName);
		JLabel newSpecNameLbl = new JLabel("Name of spending:");
		newSpecName.add(newSpecNameLbl);
		newSpecNameLbl.setAlignmentX(CENTER_ALIGNMENT);
		newSpecName.add(newSpecNameFldCont);
		newSpecNameFldCont.add(newSpecNameFld);
		newSpecBox.add(newSpecShow);
		newSpecBox.add(new JLabel("Amount:"));

		editBox.add(editNameBox);
		editNameLbl = new JLabel();
		editNameBox.add(editNameLbl);
		editNameLbl.setAlignmentX(CENTER_ALIGNMENT);
		editNameBox.add(editNameCont);
		editNameCont.add(editName);
		editSpecRight.add(editSpecAmtBox);
		JLabel editSpecAmtLbl = new JLabel("Spending amout:");
		editSpecAmtBox.add(editSpecAmtLbl);
		editSpecAmtBox.add(editSpecAmtCont);
		editSpecAmtCont.add(editSpecAmt);
		editSpecRight.add(editSpecShow);

		projSettingsPromptPnl.add(projSettingsPromptBox, BorderLayout.PAGE_START);
		projSettingsPromptBox.add(projFastForward);
		projFastForward.add(new JLabel("Fast forward: "));
		projFastForward.add(projFFTimeAmt);
		projFastForward.add(projFFUOT);
		projSettingsPromptBox.add(new JSeparator());
		projSettingsPromptBox.add(projEarningsSett);
		projEarningsSett.add(new JLabel("Income rate: "));
		projEarningsSett.add(projEarningsAmt);
		projEarningsSett.add(new JLabel(" every "));
		projEarningsSett.add(projEarningsTimeAmt);
		projEarningsSett.add(projEarningsUOT);
		projSettingsPromptBox.add(new JSeparator());
		projSettingsPromptBox.add(Box.createVerticalStrut(10));
		JLabel projCats = new JLabel("Category Spendings");
		projSettingsPromptBox.add(projCats);
		projSettingsPromptBox.add(projDynamic);
		projSettingsPromptBox.add(new JSeparator());
		projSettingsPromptBox.add(projFactorIn);

		JLabel editUnusedColorLbl = new JLabel("Unused money - edit color:");
		editUnusedColorBox.add(editUnusedColorLbl);
		editUnusedColorLbl.setAlignmentX(CENTER_ALIGNMENT);
		editUnusedColorBox.add(new JSeparator());
	}

	private void refreshCategoriesDisplay()
	{
		selectCatCombo.removeAllItems();
		for (Category cat: categories) {
			selectCatCombo.addItem(cat.name);
		}
	}

	private int i;
	private void readBudgetSettings() throws Exception {
		totalEarnings = roundCent(Double.parseDouble(fileContent.get(0)[1]));
		totalSpendings = roundCent(Double.parseDouble(fileContent.get(1)[1]));
		colorUnusedStr = fileContent.get(2)[1]; colorUnused = getColor(colorUnusedStr);
		String[] thisEntry;
		double catSpendings = 0;
		for (i = 4; i < fileContent.size(); i++) {
			thisEntry = fileContent.get(i);
			if (thisEntry[1].equals("POSSIBLE SPENDINGS")) {
				i++;
				break;
			}
			Category c = new Category();
			c.name = thisEntry[0];
			c.abbr = thisEntry[1].split(",")[0];
			c.spending = Double.parseDouble(thisEntry[1].split(",")[1]);
			catSpendings += c.spending;
			c.colorStr = thisEntry[1].split(",")[2]; c.color = getColor(c.colorStr);
			c.memo = thisEntry[1].split(",")[3];
			categories.add(c);
		}
		if (roundCent(catSpendings) != totalSpendings) {
			throw new Exception("File Error: Category spendings do not add up to total spendings");
		}
		for (; i < fileContent.size(); i++) {
			thisEntry = fileContent.get(i);
			if (thisEntry[1].equals("FORECAST CONFIG")) {
				i++;
				break;
			}
			SpecCategory c = new SpecCategory();
			c.name = thisEntry[0];
			c.abbr = thisEntry[1].split(",")[0];
			c.spending = Double.parseDouble(thisEntry[1].split(",")[1]);
			c.colorStr = thisEntry[1].split(",")[2]; c.color = getColor(c.colorStr);
			c.showInPie = thisEntry[1].split(",")[3].equals("show") ? true : false;
			specCategories.add(c);
		}
		double totalShownSpec = 0;
		for (SpecCategory sc: specCategories) {
			if (sc.showInPie) {
				totalShownSpec += sc.spending;
				if (totalEarnings - totalSpendings < totalShownSpec) {
					sc.showInPie = false;
					totalShownSpec -= sc.spending;
				}
			}
		}
		thisEntry = fileContent.get(i);
		projTimeAmount = Integer.parseInt(thisEntry[1].split("-")[0]);
		projUnitOfTimeStr = thisEntry[1].split("-")[1];
		i++;
		thisEntry = fileContent.get(i);
		factorInStr = thisEntry[1];
		i++;
		thisEntry = fileContent.get(i);
		forecastEarningsAmt = Double.parseDouble(thisEntry[1].split(",")[0]);
		forecastEarningsTimeAmount = Integer.parseInt(thisEntry[1].split(",")[1].split("-")[0]);
		forecastEarningsUnitOfTimeStr = thisEntry[1].split(",")[1].split("-")[1]; forecastEarningsUnitOfTime = parseUnitOfTime(forecastEarningsUnitOfTimeStr);
		i++;
		for (; i < fileContent.size(); i++) {
			thisEntry = fileContent.get(i);
			ProjectionSetting projSetting = new ProjectionSetting();
			projectionSettings.add(projSetting);
			projSetting.category = getProjCategory(thisEntry[0]);
			projSetting.amount = Double.parseDouble(thisEntry[1].split(",")[0]);
			projSetting.timeAmount = Integer.parseInt(thisEntry[1].split(",")[1].split("-")[0]);
			projSetting.unitOfTimeStr = thisEntry[1].split(",")[1].split("-")[1]; projSetting.unitOfTime = parseUnitOfTime(projSetting.unitOfTimeStr);
			catProjMap.put(projSetting.category, projSetting);
		}
	}

	private void initializeColor() {
		colorCombo.setSelectedIndex(0);
		colorCombo.setEnabled(true);
		redFld.setText("");
		greenFld.setText("");
		blueFld.setText("");
	}

	private UnitOfTime parseUnitOfTime(String string) {
		if (string.equals("day")) {
			return UnitOfTime.DAY;
		}
		if (string.equals("week")) {
			return UnitOfTime.WEEK;
		}
		if (string.equals("month")) {
			return UnitOfTime.MONTH;
		}
		if (string.equals("year")) {
			return UnitOfTime.YEAR;
		}
		return null;
	}

	private Category getProjCategory(String string) throws Exception {
		for (Category c: categories) {
			if (c.name.equals(string)) {
				return c;
			}
		}
		throw new Exception("Error: Category not found for '" + string + "'");
	}

	private JButton earn = new JButton("Earn");
	private JButton spend = new JButton("Spend");
	private JButton newCat = new JButton("New category");
	private JButton newSpec = new JButton("New possible spending");
	private int bttnHeight = 17;
	private Font buttonFont = new Font("arial", Font.PLAIN, 11);
	private void initializeButtons() throws IOException {
		earn.setPreferredSize(new Dimension(56, bttnHeight));
		earn.setFont(buttonFont);
		spend.setPreferredSize(new Dimension(65, bttnHeight));
		spend.setFont(buttonFont);
		newCat.setPreferredSize(new Dimension(103, bttnHeight));
		newCat.setFont(buttonFont);
		newSpec.setPreferredSize(new Dimension(147, bttnHeight));
		newSpec.setFont(buttonFont);
		earn.addActionListener(new ActionListener() {
			String newEarnings;
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					newEarnings = JOptionPane.showInputDialog(MainFrame.this, "Earned this much money:");
					if (newEarnings == null) {
						return;
					}
					double addedEarnings = Double.parseDouble(newEarnings);
					if (totalEarnings + addedEarnings - totalSpendings < 0) {
						throw new Exception("Error: Your unused money would be negative");
					}
					totalEarnings = roundCent(totalEarnings + addedEarnings);
					updateFile();
					inputs.removeAll();
					buildInputs();
					MainFrame.this.validate();
					MainFrame.this.repaint();
					JOptionPane.showMessageDialog(MainFrame.this, "Added to earnings!\nCurrent total earnings: " + totalEarnings);
				} catch (Exception e1) {
					if (e1 instanceof NumberFormatException) {
						JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid amount", "NumberFormatException", JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		spend.addActionListener(new ActionListener() {
			String newSpending;
			Category spendCat;
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (categories.size() == 0) {
						throw new Exception("You don't have any categories to spend money in");
					}
					newSpending = JOptionPane.showInputDialog(MainFrame.this, spendPanel);
					if (newSpending == null) {
						return;
					}
					double addedSpending = Double.parseDouble(newSpending);
					if (totalEarnings - totalSpendings - addedSpending < 0) {
						throw new Exception("Error: Your unused money would be negative");
					}
					spendCat = findCategory(selectCatCombo.getSelectedItem(), categories);
					if (spendCat.spending + addedSpending < 0) {
						throw new Exception("Error: Your spendings for the category '" + selectCatCombo.getSelectedItem() + "' would be negative");
					}
					totalSpendings = roundCent(totalSpendings + addedSpending);
					spendCat.spending = roundCent(spendCat.spending + addedSpending);
					spendCat.memo = "last-edit " + roundCent(addedSpending) + " " + DATE_FORMAT.format(new Date());
					updateFile();
					inputs.removeAll();
					buildInputs();
					MainFrame.this.validate();
					MainFrame.this.repaint();
					JOptionPane.showMessageDialog(MainFrame.this, "Placed in records!");
				} catch (Exception e1) {
					if (e1 instanceof NumberFormatException) {
						JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid amount", "NumberFormatException", JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			private Category findCategory(Object object, ArrayList<Category> list) throws Exception {
				for (Category c: list) {
					if (c.name.equals(object)) {
						return c;
					}
				}
				throw new Exception("Error: Category not found for '" + object + "'");
			}
		});
		newCat.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				initializeColor();
				newCatBox.add(colorBigBox);
				int newCatDialog = JOptionPane.showConfirmDialog(MainFrame.this, newCatBox, "New Category", JOptionPane.OK_CANCEL_OPTION);
				if (newCatDialog == JOptionPane.OK_OPTION) {
					try {
						if (catName.getText().length() == 0) {
							throw new Exception("Error: Category name is missing");
						}
						if (!checkName(catName.getText())) {
							throw new Exception("Error: Category name cannot contain a colon or comma");
						}
						for (Category c: categories) {
							if (c.name.equals(catName.getText())) {
								throw new Exception("Error: Category with that name already exists");
							}
						}
						Color theColor;
						if (colorCombo.isEnabled()) {
							theColor = getColor((String)colorCombo.getSelectedItem());
						} else {
							theColor = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
						}
						Category newCategory = new Category();
						newCategory.color = theColor;
						newCategory.colorStr = colorCombo.isEnabled() ? (String)colorCombo.getSelectedItem() : "" + Integer.parseInt(redFld.getText()) + '-' + Integer.parseInt(greenFld.getText()) + '-' + Integer.parseInt(blueFld.getText());
						newCategory.name = catName.getText();
						newCategory.abbr = abbrev(newCategory.name);
						newCategory.spending = 0;
						newCategory.memo = "last-edit n/a";
						categories.add(newCategory);
						ProjectionSetting newProjSett = new ProjectionSetting();
						newProjSett.category = newCategory;
						projectionSettings.add(newProjSett);
						catProjMap.put(newCategory, newProjSett);
						updateFile();
						refreshCategoriesDisplay();
						inputs.removeAll();
						buildInputs();
						MainFrame.this.validate();
						MainFrame.this.repaint();
						JOptionPane.showMessageDialog(MainFrame.this, "Added new category!");
					} catch (Exception e1) {
						if (e1 instanceof NumberFormatException || e1 instanceof IllegalArgumentException) {
							JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid input for color", "Number Format Exception", JOptionPane.ERROR_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		});
		newSpec.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					double newSpecAmount;
					try {
						initializeColor();
						newSpecBoxTop.add(colorBigBox);
						String newSpecAmountStr = JOptionPane.showInputDialog(MainFrame.this, newSpecBox, "New Possible Spending", JOptionPane.PLAIN_MESSAGE);
						if (newSpecAmountStr == null) {
							return;
						}
						newSpecAmount = Double.parseDouble(newSpecAmountStr);
						if (newSpecAmount < 0) {
							throw new NumberFormatException();
						}
					} catch (NumberFormatException e1) {
						throw new Exception("Error: Not a valid amount");
					}
					if (newSpecNameFld.getText().length() == 0) {
						throw new Exception("Error: Spending name is missing");
					}
					if (!checkName(newSpecNameFld.getText())) {
						throw new Exception("Error: Category name cannot contain a colon or comma");
					}
					double totalShownSpec = 0;
					for (SpecCategory sc: specCategories) {
						if (sc.name.equals(newSpecNameFld.getText())) {
							throw new Exception("Error: Spending with that name already exists");
						}
						if (sc.showInPie) {
							totalShownSpec += sc.spending;
						}
					}
					Color theColor;
					if (colorCombo.isEnabled()) {
						theColor = getColor((String)colorCombo.getSelectedItem());
					} else {
						theColor = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
					}
					SpecCategory theSpec = new SpecCategory();
					theSpec.color = theColor;
					theSpec.colorStr = colorCombo.isEnabled() ? (String)colorCombo.getSelectedItem() : "" + Integer.parseInt(redFld.getText()) + '-' + Integer.parseInt(greenFld.getText()) + '-' + Integer.parseInt(blueFld.getText());
					theSpec.name = newSpecNameFld.getText();
					theSpec.abbr = abbrev(theSpec.name);
					theSpec.spending = roundCent(newSpecAmount);
					theSpec.showInPie = newSpecShow.isSelected() ? true : false;
					if (theSpec.showInPie && (viewOption == ViewOption.FORECAST ? forecastEarningsCalcAmt - forecastSpendingsCalcAmt : (totalEarnings - totalSpendings)) < totalShownSpec + theSpec.spending) {
						JOptionPane.showMessageDialog(MainFrame.this, "There's not enough room in your pie to show this spending. Changed 'Show in pie' to false.");
						theSpec.showInPie = false;
					}
					specCategories.add(theSpec);
					updateFile();
					inputs.removeAll();
					buildInputs();
					MainFrame.this.validate();
					MainFrame.this.repaint();
					JOptionPane.showMessageDialog(MainFrame.this, "Added new possible spending!");
				} catch (Exception e1) {
					if (e1 instanceof NumberFormatException || e1 instanceof IllegalArgumentException) {
						JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid input for color", "Number Format Exception", JOptionPane.ERROR_MESSAGE);
					} else {
						JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		editUnusedColor = new JButton((Icon) new ImageIcon(ImageIO.read(new File("edit.png"))));
		editUnusedColor.setPreferredSize(new Dimension(17, 17));
		editUnusedColor.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		editUnusedColor.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {	}
			@Override
			public void mousePressed(MouseEvent e) {
				editUnusedColor.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				editUnusedColor.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
		});
		editUnusedColor.setContentAreaFilled(false);
		editUnusedColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (colorUnusedStr.indexOf("-") == -1) {
					colorCombo.setEnabled(true);
					try {
						colorCombo.setSelectedIndex(getComboBoxSelectedIndex(colorUnusedStr));
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					redFld.setText("");
					greenFld.setText("");
					blueFld.setText("");
				} else {
					colorCombo.setSelectedIndex(0);
					colorCombo.setEnabled(false);
					redFld.setText(colorUnusedStr.split("-")[0]);
					greenFld.setText(colorUnusedStr.split("-")[1]);
					blueFld.setText(colorUnusedStr.split("-")[2]);
				}
				editUnusedColorBox.add(colorBigBox);
				int editClick = JOptionPane.showConfirmDialog(MainFrame.this, editUnusedColorBox, "Edit Color", JOptionPane.OK_CANCEL_OPTION);
				if (editClick == JOptionPane.OK_OPTION) {
					try {
						Color theColor;
						if (colorCombo.isEnabled()) {
							theColor = getColor((String)colorCombo.getSelectedItem());
						} else {
							theColor = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
						}
						colorUnused = theColor;
						colorUnusedStr = colorCombo.isEnabled() ? (String)colorCombo.getSelectedItem() : "" + Integer.parseInt(redFld.getText()) + '-' + Integer.parseInt(greenFld.getText()) + '-' + Integer.parseInt(blueFld.getText());
						updateFile();
						MainFrame.this.validate();
						MainFrame.this.repaint();
						JOptionPane.showMessageDialog(MainFrame.this, "Changed display color!");
					} catch (Exception e1) {
						if (e1 instanceof NumberFormatException || e1 instanceof IllegalArgumentException) {
							JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid input for color", "Number Format Exception", JOptionPane.ERROR_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}});
	}

	protected boolean checkName(String text) {
		return text.indexOf((int)':') == -1 && text.indexOf((int)',') == -1;
	}

	protected void makeScreen(ViewOption viewOption) throws Exception {
		if (viewOption == ViewOption.FORECAST) {
			int prompt = promptProjSettings(true);
			if (prompt == JOptionPane.OK_OPTION) {
				while (!checkForecastFields()) {
					if (promptProjSettings(false) == JOptionPane.CANCEL_OPTION) {
						(this.viewOption == ViewOption.COMPLETE_BUDGET ? viewCurrent : viewUnused).setSelected(true);
						return;
					}
				}
				updateProjSettings();
			} else {
				(this.viewOption == ViewOption.COMPLETE_BUDGET ? viewCurrent : viewUnused).setSelected(true);
				return;
			}
		}
		this.viewOption = viewOption;
		inputs.removeAll();
		buildInputs();
		earn.setEnabled(viewOption != ViewOption.FORECAST ? true: false);
		spend.setEnabled(viewOption == ViewOption.COMPLETE_BUDGET ? true: false);
		newCat.setEnabled(viewOption == ViewOption.COMPLETE_BUDGET ? true: false);
		validate();
		repaint();
	}

	private String[] userDefColor;
	private Color getColor(String colorStr) {
		if (colorStr.equals("black")) {
			return Color.BLACK;
		}
		if (colorStr.equals("blue")) {
			return Color.BLUE;
		}
		if (colorStr.equals("cyan")) {
			return Color.CYAN;
		}
		if (colorStr.equals("dark gray")) {
			return Color.DARK_GRAY;
		}
		if (colorStr.equals("gray")) {
			return Color.GRAY;
		}
		if (colorStr.equals("green")) {
			return Color.GREEN;
		}
		if (colorStr.equals("light gray")) {
			return Color.LIGHT_GRAY;
		}
		if (colorStr.equals("magenta")) {
			return Color.MAGENTA;
		}
		if (colorStr.equals("orange")) {
			return Color.ORANGE;
		}
		if (colorStr.equals("pink")) {
			return Color.PINK;
		}
		if (colorStr.equals("red")) {
			return Color.RED;
		}
		if (colorStr.equals("white")) {
			return Color.WHITE;
		}
		if (colorStr.equals("yellow")) {
			return Color.YELLOW;
		}
		userDefColor = colorStr.split("-");
		return new Color(Integer.parseInt(userDefColor[0]), Integer.parseInt(userDefColor[1]), Integer.parseInt(userDefColor[2]));
	}

	private double roundCent(double d) {
		return ((double)(Math.round(d * 100))) / 100;
	}

	private Box inputsLabels = new Box(BoxLayout.Y_AXIS);
	private JButton editUnusedColor;
	private JLabel earningsLbl = new JLabel();
	private JLabel spendingsLbl = new JLabel();
	private JLabel unusedLbl = new JLabel();
	private void buildInputs() throws Exception {
		Box inputsBox = new Box(BoxLayout.Y_AXIS);
		inputs.add(inputsBox, BorderLayout.PAGE_START);
		if (viewOption != ViewOption.FORECAST) {
			inputsBox.add(editUnusedColor);
			inputsBox.add(inputsLabels);
			inputsLabels.setAlignmentX(CENTER_ALIGNMENT);
			inputsLabels.removeAll();
			earningsLbl.setText("Total earnings: " + totalEarnings);
			if (viewOption == ViewOption.COMPLETE_BUDGET) {
				inputsLabels.add(earningsLbl);
				spendingsLbl.setText("Total spendings: " + totalSpendings);
				inputsLabels.add(spendingsLbl);
			}
			unusedLbl.setText("Unused money: " + roundCent(totalEarnings - totalSpendings));
			inputsLabels.add(unusedLbl);
			inputsBox.add(new JSeparator());
			if (viewOption == ViewOption.COMPLETE_BUDGET) {
				JLabel catMarker = new JLabel("Categories");
				inputsBox.add(catMarker);
				catMarker.setFont(new Font("arial", Font.BOLD, 13));
				catMarker.setAlignmentX(CENTER_ALIGNMENT);
				int categoryNum = 0;
				for (final Category category: categories) {
					final int categoryNumFnl = categoryNum;
					JPanel catPanel = new JPanel();
					inputsBox.add(catPanel);
					catPanel.add(new JPanel() {
						@Override
						public void paintComponent(Graphics g) {
							g.setColor(category.color);
							g.fillRect(0, 0, 28, 28);
						}
					});
					JLabel catDetails = new JLabel(category.abbr + "  " + getPercent(category.spending, totalEarnings) + "%  " + category.spending);
					catPanel.add(catDetails);
					catDetails.setFont(new Font("arial", Font.PLAIN, 11));
					final JButton edit = new JButton((Icon) new ImageIcon(ImageIO.read(new File("edit.png"))));
					edit.setPreferredSize(new Dimension(17, 17));
					edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
					edit.addMouseListener(new MouseListener() {
						@Override
						public void mouseClicked(MouseEvent e) {	}
						@Override
						public void mousePressed(MouseEvent e) {
							edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
						}
						@Override
						public void mouseReleased(MouseEvent e) {
							edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
						}
						@Override
						public void mouseEntered(MouseEvent e) {}
						@Override
						public void mouseExited(MouseEvent e) {}
					});
					edit.setContentAreaFilled(false);
					catPanel.add(edit);
					edit.addActionListener(new ActionListener() {
						JComboBox move = new JComboBox(new String[]{"Don't rearrange", "Move up", "Move down"});
						@Override
						public void actionPerformed(ActionEvent e) {
							editName.setText(category.name);
							if (category.colorStr.indexOf("-") == -1) {
								colorCombo.setEnabled(true);
								try {
									colorCombo.setSelectedIndex(getComboBoxSelectedIndex(category.colorStr));
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
									return;
								}
								redFld.setText("");
								greenFld.setText("");
								blueFld.setText("");
							} else {
								colorCombo.setSelectedIndex(0);
								colorCombo.setEnabled(false);
								redFld.setText(category.colorStr.split("-")[0]);
								greenFld.setText(category.colorStr.split("-")[1]);
								blueFld.setText(category.colorStr.split("-")[2]);
							}
							editNameLbl.setText("Category name:");
							editBox.add(colorBigBox);
							JPanel pnl = new JPanel();
							pnl.add(move);
							editBox.add(pnl);
							int editClick = JOptionPane.showOptionDialog(MainFrame.this, editBox, "Edit Category", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK", "Remove Category", "Cancel"}, null);
							editBox.remove(pnl);
							if (editClick == JOptionPane.YES_OPTION) {
								boolean contains = false;
								for (Category c: categories) {
									if (editName.getText().equals(c.name)) {
										if (!contains) {
											contains = true;
										} else {
											JOptionPane.showMessageDialog(MainFrame.this, "Error: Category with that name already exists", "Error", JOptionPane.ERROR_MESSAGE);
										}
									}
								}
								category.name = editName.getText();
								category.abbr = abbrev(category.name);
								if (colorCombo.isEnabled()) {
									try {
										category.color = getColor((String)colorCombo.getSelectedItem());
									} catch (Exception e1) {
										JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
										return;
									}
									category.colorStr = (String)colorCombo.getSelectedItem();
								} else {
									category.color = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
									category.colorStr = redFld.getText() + '-' + greenFld.getText() + '-' + blueFld.getText();
								}
								switch (move.getSelectedIndex()) {
								case 1: if (categoryNumFnl > 0) {
									Collections.swap(categories, categoryNumFnl - 1, categoryNumFnl);
									Collections.swap(projectionSettings, categoryNumFnl - 1, categoryNumFnl);
								} break;
								case 2: if (categoryNumFnl < categories.size() - 1) {
									Collections.swap(categories, categoryNumFnl, categoryNumFnl + 1);
									Collections.swap(projectionSettings, categoryNumFnl, categoryNumFnl + 1);
								}
								}
								try {
									updateFile();
								} catch (IOException e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
									return;
								}
								refreshCategoriesDisplay();
								inputs.removeAll();
								try {
									buildInputs();
								} catch (IOException e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
									return;
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
									return;
								}
								MainFrame.this.validate();
								MainFrame.this.repaint();
								JOptionPane.showMessageDialog(MainFrame.this, "Updated category!");
							} else if (editClick == JOptionPane.NO_OPTION) {
								totalSpendings = roundCent(totalSpendings - category.spending);
								categories.remove(category);
								projectionSettings.remove(catProjMap.get(category));
								catProjMap.remove(category);
								try {
									updateFile();
								} catch (IOException e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
									return;
								}
								refreshCategoriesDisplay();
								inputs.removeAll();
								try {
									buildInputs();
								} catch (IOException e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
									return;
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
									return;
								}
								MainFrame.this.validate();
								MainFrame.this.repaint();
								JOptionPane.showMessageDialog(MainFrame.this, "Removed category!\n(Subtracted " + category.spending + " from total spendings.)");
							}
						}
					});
					categoryNum++;
				}
				if (categories.size() == 0) {
					JLabel empty = new JLabel("<empty>");
					inputsBox.add(empty);
					empty.setAlignmentX(CENTER_ALIGNMENT);
				}
				inputsBox.add(new JSeparator());
			}
			JLabel specMarker = new JLabel("Possible Spendings");
			inputsBox.add(specMarker);
			specMarker.setAlignmentX(CENTER_ALIGNMENT);
			double grossSpec = 0;
			boolean changed = false;
			int categoryNum = 0;
			for (final SpecCategory possCat: specCategories) {
				final int categoryNumFnl = categoryNum;
				if (possCat.showInPie && grossSpec + possCat.spending > totalEarnings - totalSpendings) {
					possCat.showInPie = false;
					changed = true;
				}
				if (possCat == specCategories.get(specCategories.size() - 1) && changed) {
					updateFile();
					inputs.removeAll();
					buildInputs();
					MainFrame.this.validate();
					MainFrame.this.repaint();
					return;
				}
				grossSpec += possCat.spending;
				JPanel specPanel = new JPanel();
				inputsBox.add(specPanel);
				specPanel.add(new JPanel() {
					@Override
					public void paintComponent(Graphics g) {
						g.setColor(possCat.color);
						if (possCat.showInPie) {
							g.fillRect(0, 0, 28, 28);
						} else {
							g.drawRect(0, 0, 9, 9);
						}
					}
				});
				JLabel catDetails = new JLabel(possCat.abbr + "  " + (possCat.showInPie ? getPercent(possCat.spending, viewOption == ViewOption.COMPLETE_BUDGET ? totalEarnings : totalEarnings - totalSpendings) + "%  " : "") + possCat.spending);
				if (!possCat.showInPie) {
					catDetails.setForeground(Color.GRAY);
				}
				specPanel.add(catDetails);
				catDetails.setFont(new Font("arial", Font.PLAIN, 11));
				final JButton edit = new JButton((Icon) new ImageIcon(ImageIO.read(new File("edit.png"))));
				edit.setPreferredSize(new Dimension(17, 17));
				edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				edit.addMouseListener(new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {	}
					@Override
					public void mousePressed(MouseEvent e) {
						edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
					}
					@Override
					public void mouseReleased(MouseEvent e) {
						edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
					}
					@Override
					public void mouseEntered(MouseEvent e) {}
					@Override
					public void mouseExited(MouseEvent e) {}
				});
				edit.setContentAreaFilled(false);
				specPanel.add(edit);
				edit.addActionListener(new ActionListener() {
					JComboBox move = new JComboBox(new String[]{"Don't rearrange", "Move up", "Move down"});
					@Override
					public void actionPerformed(ActionEvent e) {
						editName.setText(possCat.name);
						if (possCat.colorStr.indexOf("-") == -1) {
							colorCombo.setEnabled(true);
							try {
								colorCombo.setSelectedIndex(getComboBoxSelectedIndex(possCat.colorStr));
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							redFld.setText("");
							greenFld.setText("");
							blueFld.setText("");
						} else {
							colorCombo.setSelectedIndex(0);
							colorCombo.setEnabled(false);
							redFld.setText(possCat.colorStr.split("-")[0]);
							greenFld.setText(possCat.colorStr.split("-")[1]);
							blueFld.setText(possCat.colorStr.split("-")[2]);
						}
						editNameLbl.setText("Spending name:");
						editBox.add(colorBigBox);
						editBox.add(editSpecRight);
						JPanel pnl = new JPanel();
						pnl.add(move);
						editBox.add(pnl);
						editSpecAmt.setText("" + possCat.spending);
						editSpecShow.setSelected(possCat.showInPie ? true : false);
						int editClick = JOptionPane.showOptionDialog(MainFrame.this, editBox, "Edit Possible Spending", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK", "Remove Spending", "Cancel"}, null);
						editBox.remove(editSpecRight);
						editBox.remove(pnl);
						if (editClick == JOptionPane.YES_OPTION) {
							boolean contains = false;
							for (SpecCategory sc: specCategories) {
								if (editName.getText().equals(sc.name)) {
									if (!contains) {
										contains = true;
									} else {
										JOptionPane.showMessageDialog(MainFrame.this, "Error: Possible spending with that name already exists", "Error", JOptionPane.ERROR_MESSAGE);
									}
								}
							}
							possCat.name = editName.getText();
							possCat.abbr = abbrev(possCat.name);
							if (colorCombo.isEnabled()) {
								try {
									possCat.color = getColor((String)colorCombo.getSelectedItem());
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
									return;
								}
								possCat.colorStr = (String)colorCombo.getSelectedItem();
							} else {
								possCat.color = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
								possCat.colorStr = redFld.getText() + '-' + greenFld.getText() + '-' + blueFld.getText();
							}
							try {
								double inputAmt = Double.parseDouble(editSpecAmt.getText());
								if (inputAmt < 0) {
									throw new Exception("Error: Amount cannot be negative");
								}
								possCat.spending = roundCent(inputAmt);
							} catch (Exception e1) {
								if (e1 instanceof NumberFormatException) {
									JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid amont", "Error", JOptionPane.ERROR_MESSAGE);
								} else {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
								}
								return;
							}
							possCat.showInPie = editSpecShow.isSelected() ? true : false;
							if (possCat.showInPie) {
								double totalShownSpec = 0;
								for (SpecCategory sc: specCategories) {
									if (sc != possCat && sc.showInPie) {
										totalShownSpec += sc.spending;
									}
								}
								if (totalEarnings - totalSpendings - totalShownSpec - possCat.spending < 0) {
									JOptionPane.showMessageDialog(MainFrame.this, "There's not enough room in the pie to show this spending. Changed 'Show in pie' to false.");
									possCat.showInPie = false;
								}
							}
							switch (move.getSelectedIndex()) {
							case 1: if (categoryNumFnl > 0) {
								Collections.swap(specCategories, categoryNumFnl - 1, categoryNumFnl);
							} break;
							case 2: if (categoryNumFnl < specCategories.size() - 1) {
								Collections.swap(specCategories, categoryNumFnl, categoryNumFnl + 1);
							}
							}
							try {
								updateFile();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							}
							inputs.removeAll();
							try {
								buildInputs();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							MainFrame.this.validate();
							MainFrame.this.repaint();
							JOptionPane.showMessageDialog(MainFrame.this, "Updated possible spending!");
						} else if (editClick == JOptionPane.NO_OPTION) {
							specCategories.remove(possCat);
							try {
								updateFile();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							}
							inputs.removeAll();
							try {
								buildInputs();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							MainFrame.this.validate();
							MainFrame.this.repaint();
							JOptionPane.showMessageDialog(MainFrame.this, "Removed possible spending!");
						}
					}
				});
				categoryNum++;
			}
			if (specCategories.size() == 0) {
				JLabel empty = new JLabel("<empty>");
				inputsBox.add(empty);
				empty.setAlignmentX(CENTER_ALIGNMENT);
			}
		} else {
			forecastEarningsCalcAmt = roundCent(project(forecastEarningsAmt, forecastEarningsTimeAmount, forecastEarningsUnitOfTime) + (Boolean.parseBoolean(factorInStr) ? totalEarnings : 0));
			inputsBox.add(inputsLabels);
			inputsLabels.setAlignmentX(CENTER_ALIGNMENT);
			inputsLabels.removeAll();
			inputsLabels.add(new JLabel("Fast forward: " + projTimeAmount + " " + projUnitOfTimeStr + (projTimeAmount > 1 ? "s" : "")));
			JLabel calcEarningsLbl = new JLabel("Total earnings: " + forecastEarningsCalcAmt);
			inputsLabels.add(calcEarningsLbl);
			forecastSpendingsCalcAmt = (Boolean.parseBoolean(factorInStr) ? totalSpendings : 0);
			ArrayList<JPanel> forecastPanels = new ArrayList<JPanel>();
			for (final ProjectionSetting sett: projectionSettings) {
				forecastSpendingsCalcAmt = roundCent(forecastSpendingsCalcAmt + sett.calcAmt);
				JPanel catPanel = new JPanel();
				forecastPanels.add(catPanel);
				catPanel.add(new JPanel() {
					@Override
					public void paintComponent(Graphics g) {
						g.setColor(sett.category.color);
						g.fillRect(0, 0, 28, 28);
					}
				});
				JLabel catDetails = new JLabel(sett.category.abbr + "  " + (roundCent(forecastEarningsCalcAmt - forecastSpendingsCalcAmt) >= 0 ? getPercent(sett.calcAmt + (Boolean.parseBoolean(factorInStr) ? sett.category.spending : 0), forecastEarningsCalcAmt) + "%  " : "") + (sett.calcAmt + (Boolean.parseBoolean(factorInStr) ? sett.category.spending : 0)));
				catPanel.add(catDetails);
				catDetails.setFont(new Font("arial", Font.PLAIN, 11));
			}
			JLabel calcSpendingsLbl = new JLabel("Total spendings: " + forecastSpendingsCalcAmt);
			inputsLabels.add(calcSpendingsLbl);
			JLabel calcUnusedLbl = new JLabel("Unused money: " + roundCent(forecastEarningsCalcAmt - forecastSpendingsCalcAmt));
			inputsLabels.add(calcUnusedLbl);
			inputsBox.add(new JSeparator());
			JLabel catMarker = new JLabel("Categories");
			inputsBox.add(catMarker);
			catMarker.setFont(new Font("arial", Font.BOLD, 13));
			catMarker.setAlignmentX(CENTER_ALIGNMENT);
			for (JPanel forecastPanel: forecastPanels) {
				inputsBox.add(forecastPanel);
			}
			if (forecastPanels.size() == 0) {
				JLabel empty = new JLabel("<empty>");
				inputsBox.add(empty);
				empty.setAlignmentX(CENTER_ALIGNMENT);
			}
			inputsBox.add(new JSeparator());
			JLabel specMarker = new JLabel("Possible Spendings");
			inputsBox.add(specMarker);
			specMarker.setAlignmentX(CENTER_ALIGNMENT);
			double grossSpec = 0;
			boolean changed = false;
			for (final SpecCategory possCat: specCategories) {
				if (possCat.showInPie && grossSpec + possCat.spending > forecastEarningsCalcAmt - forecastSpendingsCalcAmt) {
					possCat.showInPie = false;
					changed = true;
				}
				if (possCat == specCategories.get(specCategories.size() - 1) && changed) {
					updateFile();
					inputs.removeAll();
					buildInputs();
					return;
				}
				grossSpec += possCat.spending;
				JPanel specPanel = new JPanel();
				inputsBox.add(specPanel);
				specPanel.add(new JPanel() {
					@Override
					public void paintComponent(Graphics g) {
						g.setColor(possCat.color);
						if (possCat.showInPie) {
							g.fillRect(0, 0, 28, 28);
						} else {
							g.drawRect(0, 0, 9, 9);
						}
					}
				});
				JLabel catDetails = new JLabel(possCat.abbr + "  " + (possCat.showInPie ? getPercent(possCat.spending, this.forecastEarningsCalcAmt) + "%  " : "") + possCat.spending);
				if (!possCat.showInPie) {
					catDetails.setForeground(Color.GRAY);
				}
				specPanel.add(catDetails);
				catDetails.setFont(new Font("arial", Font.PLAIN, 11));
				final JButton edit = new JButton((Icon) new ImageIcon(ImageIO.read(new File("edit.png"))));
				edit.setPreferredSize(new Dimension(17, 17));
				edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				edit.addMouseListener(new MouseListener() {
					@Override
					public void mouseClicked(MouseEvent e) {	}
					@Override
					public void mousePressed(MouseEvent e) {
						edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
					}
					@Override
					public void mouseReleased(MouseEvent e) {
						edit.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
					}
					@Override
					public void mouseEntered(MouseEvent e) {}
					@Override
					public void mouseExited(MouseEvent e) {}
				});
				edit.setContentAreaFilled(false);
				specPanel.add(edit);
				edit.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						editName.setText(possCat.name);
						if (possCat.colorStr.indexOf("-") == -1) {
							colorCombo.setEnabled(true);
							try {
								colorCombo.setSelectedIndex(getComboBoxSelectedIndex(possCat.colorStr));
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							redFld.setText("");
							greenFld.setText("");
							blueFld.setText("");
						} else {
							colorCombo.setSelectedIndex(0);
							colorCombo.setEnabled(false);
							redFld.setText(possCat.colorStr.split("-")[0]);
							greenFld.setText(possCat.colorStr.split("-")[1]);
							blueFld.setText(possCat.colorStr.split("-")[2]);
						}
						editNameLbl.setText("Spending name:");
						editBox.add(colorBigBox);
						editBox.add(editSpecRight);
						editSpecAmt.setText("" + possCat.spending);
						editSpecShow.setSelected(possCat.showInPie ? true : false);
						int editClick = JOptionPane.showOptionDialog(MainFrame.this, editBox, "Edit Possible Spending", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"OK", "Remove Spending", "Cancel"}, null);
						editBox.remove(editSpecRight);
						if (editClick == JOptionPane.YES_OPTION) {
							boolean contains = false;
							for (SpecCategory sc: specCategories) {
								if (editName.getText().equals(sc.name)) {
									if (!contains) {
										contains = true;
									} else {
										JOptionPane.showMessageDialog(MainFrame.this, "Error: Possible spending with that name already exists", "Error", JOptionPane.ERROR_MESSAGE);
									}
								}
							}
							possCat.name = editName.getText();
							possCat.abbr = abbrev(possCat.name);
							if (colorCombo.isEnabled()) {
								try {
									possCat.color = getColor((String)colorCombo.getSelectedItem());
								} catch (Exception e1) {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
									return;
								}
								possCat.colorStr = (String)colorCombo.getSelectedItem();
							} else {
								possCat.color = new Color(Integer.parseInt(redFld.getText()), Integer.parseInt(greenFld.getText()), Integer.parseInt(blueFld.getText()));
								possCat.colorStr = redFld.getText() + '-' + greenFld.getText() + '-' + blueFld.getText();
							}
							try {
								double inputAmt = Double.parseDouble(editSpecAmt.getText());
								if (inputAmt < 0) {
									throw new Exception("Error: Amount cannot be negative");
								}
								possCat.spending = roundCent(inputAmt);
							} catch (Exception e1) {
								if (e1 instanceof NumberFormatException) {
									JOptionPane.showMessageDialog(MainFrame.this, "Error: Not a valid amont", "Error", JOptionPane.ERROR_MESSAGE);
								} else {
									JOptionPane.showMessageDialog(MainFrame.this, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
								}
								return;
							}
							possCat.showInPie = editSpecShow.isSelected() ? true : false;
							if (possCat.showInPie) {
								double totalShownSpec = 0;
								for (SpecCategory sc: specCategories) {
									if (sc != possCat && sc.showInPie) {
										totalShownSpec += sc.spending;
									}
								}
								if (forecastEarningsCalcAmt - forecastSpendingsCalcAmt - totalShownSpec - possCat.spending < 0) {
									JOptionPane.showMessageDialog(MainFrame.this, "There's not enough room in the pie to show this spending. Changed 'Show in pie' to false.");
									possCat.showInPie = false;
								}
							}
							try {
								updateFile();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							}
							inputs.removeAll();
							try {
								buildInputs();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							MainFrame.this.validate();
							MainFrame.this.repaint();
							JOptionPane.showMessageDialog(MainFrame.this, "Updated possible spending!");
						} else if (editClick == JOptionPane.NO_OPTION) {
							specCategories.remove(possCat);
							try {
								updateFile();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							}
							inputs.removeAll();
							try {
								buildInputs();
							} catch (IOException e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
								return;
							} catch (Exception e1) {
								JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
							MainFrame.this.validate();
							MainFrame.this.repaint();
							JOptionPane.showMessageDialog(MainFrame.this, "Removed possible spending!");
						}
					}
				});
			}
			if (specCategories.size() == 0) {
				JLabel empty = new JLabel("<empty>");
				inputsBox.add(empty);
				empty.setAlignmentX(CENTER_ALIGNMENT);
			}
			inputsBox.add(new JSeparator());
			JButton editForecastSettings = new JButton("Edit Forecast Settings");
			inputsBox.add(editForecastSettings);
			editForecastSettings.setAlignmentX(CENTER_ALIGNMENT);
			editForecastSettings.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int prompt = 0;
					try {
						prompt = promptProjSettings(true);
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(MainFrame.this, e2.getClass().toString() + ": " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (prompt == JOptionPane.OK_OPTION) {
						try {
							while (!checkForecastFields()) {
								if (promptProjSettings(false) == JOptionPane.CANCEL_OPTION) {
									return;
								}
							}
							updateProjSettings();
						} catch (Exception e2) {
							JOptionPane.showMessageDialog(MainFrame.this, e2.getClass().toString() + ": " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
						try {
							updateFile();
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
							return;
						}
						inputs.removeAll();
						try {
							buildInputs();
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "IOException", JOptionPane.ERROR_MESSAGE);
							return;
						} catch (Exception e1) {
							JOptionPane.showMessageDialog(MainFrame.this, e1.getClass().toString() + ": " + e1.getMessage() , "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						MainFrame.this.validate();
						MainFrame.this.repaint();
						JOptionPane.showMessageDialog(MainFrame.this, "Updated forecast settings!");
					}
				}
			});
		}
	}

	protected int promptProjSettings(boolean reset) throws Exception {
		if (reset) {
			makeProjSettDialog();
		}
		int editProjSettClick = JOptionPane.showConfirmDialog(MainFrame.this, projSettingsPromptPnl, "Forecast Settings", JOptionPane.OK_CANCEL_OPTION);
		return editProjSettClick;
	}

	private void makeProjSettDialog() throws Exception {
		projFFTimeAmt.setText("" + projTimeAmount);
		projEarningsAmt.setText("" + forecastEarningsAmt);
		projFFUOT.setSelectedIndex(getUnitOfTimeSelectedIndex(parseUnitOfTime(projUnitOfTimeStr)));
		projEarningsTimeAmt.setText("" + forecastEarningsTimeAmount);
		projEarningsUOT.setSelectedIndex(getUnitOfTimeSelectedIndex(forecastEarningsUnitOfTime));
		projCatsMapAmt.clear();
		projCatsMapTimeAmt.clear();
		projCatsMapUnitOfTime.clear();
		projDynamic.removeAll();
		for (final ProjectionSetting sett: projectionSettings) {
			Box projSettingBox = new Box(BoxLayout.X_AXIS);
			projDynamic.add(projSettingBox);
			JPanel catNameColor = new JPanel();
			projSettingBox.add(catNameColor);
			catNameColor.add(new JPanel() {
				@Override
				public void paintComponent(Graphics g) {
					g.setColor(sett.category.color);
					g.fillRect(0, 0, 28, 28);
				}
			});
			catNameColor.add(new JLabel(sett.category.name));
			JTextField amtField = new JTextField(9);
			projSettingBox.add(amtField);
			projCatsMapAmt.put(sett, amtField);
			amtField.setText("" + sett.amount);
			projSettingBox.add(new JLabel(" every "));
			JTextField timeAmtField = new JTextField(4);
			projSettingBox.add(timeAmtField);
			projCatsMapTimeAmt.put(sett, timeAmtField);
			timeAmtField.setText("" + sett.timeAmount);
			JComboBox unitOfTimeCombo = new JComboBox(new String[]{"day", "week", "month", "year"});
			projSettingBox.add(unitOfTimeCombo);
			projCatsMapUnitOfTime.put(sett, unitOfTimeCombo);
			unitOfTimeCombo.setSelectedIndex(getUnitOfTimeSelectedIndex(sett.unitOfTime));
		}
		if (projectionSettings.size() == 0) {
			JLabel empty = new JLabel("<empty>");
			projDynamic.add(empty);
		}
		projFactorIn.setSelected(Boolean.parseBoolean(factorInStr));
	}

	protected boolean checkForecastFields() {
		String location = null;
		try {
			location = "Fast forward";
			if (Integer.parseInt(projFFTimeAmt.getText()) < 0) {
				JOptionPane.showMessageDialog(this, "Error: Fast forward - '" + projFFUOT.getSelectedItem() +  "' amount cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			location = "Income rate";
			if (Double.parseDouble(projEarningsAmt.getText()) < 0) {
				JOptionPane.showMessageDialog(this, "Error: Income rate - amount cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			if (Integer.parseInt(projEarningsTimeAmt.getText()) <= 0) {
				JOptionPane.showMessageDialog(this, "Error: Income rate - number of '" + projEarningsUOT.getSelectedItem() + "'s must be at least 1", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Error: Not a valid input for '" + location +  "'", "NumberFormatException", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		for (ProjectionSetting sett: projectionSettings) {
			try {
				location = sett.category.name;
				if (Double.parseDouble(projCatsMapAmt.get(sett).getText()) < 0) {
					JOptionPane.showMessageDialog(this, "Error: Category '" + location + "' - amount cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				if (Integer.parseInt(projCatsMapTimeAmt.get(sett).getText()) <= 0) {
					JOptionPane.showMessageDialog(this, "Error: Category '" + location + "' - number of '" + projCatsMapUnitOfTime.get(sett).getSelectedItem() + "'s must be at least 1", "Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Error: Not a valid input for category '" + location + "'", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

	protected void updateProjSettings() {
		projTimeAmount = Integer.parseInt(projFFTimeAmt.getText());
		projUnitOfTimeStr = (String) projFFUOT.getSelectedItem();
		forecastEarningsAmt = Double.parseDouble(projEarningsAmt.getText());
		forecastEarningsTimeAmount = Integer.parseInt(projEarningsTimeAmt.getText());
		forecastEarningsUnitOfTimeStr = (String) projEarningsUOT.getSelectedItem();
		forecastEarningsUnitOfTime = parseUnitOfTime(forecastEarningsUnitOfTimeStr);
		for (ProjectionSetting sett: projectionSettings) {
			sett.amount = Double.parseDouble(projCatsMapAmt.get(sett).getText());
			sett.timeAmount = Integer.parseInt(projCatsMapTimeAmt.get(sett).getText());
			sett.unitOfTimeStr = (String) projCatsMapUnitOfTime.get(sett).getSelectedItem();
			sett.unitOfTime = parseUnitOfTime(sett.unitOfTimeStr);
			sett.calcAmt = roundCent(project(sett.amount, sett.timeAmount, sett.unitOfTime));
		}
		factorInStr = projFactorIn.isSelected() ? "true" : "false";
	}

	private double project(double amt, int timeAmount, UnitOfTime unitOfTime) {
		long calTime;
		long currentTime;
		Calendar cal = Calendar.getInstance();
		int calSetting = -1;
		switch (parseUnitOfTime(projUnitOfTimeStr)) {
		case DAY: calSetting = Calendar.DAY_OF_YEAR; break;
		case WEEK: calSetting = Calendar.WEEK_OF_YEAR; break;
		case MONTH: calSetting = Calendar.MONTH; break;
		case YEAR: calSetting = Calendar.YEAR;
		}
		cal.add(calSetting, projTimeAmount);
		currentTime = new Date().getTime();
		calTime = cal.getTimeInMillis();
		double numberOfTimes = 0;
		if (unitOfTime == UnitOfTime.DAY || unitOfTime == UnitOfTime.WEEK) {
			numberOfTimes = (double)(calTime - currentTime) / (unitOfTime == UnitOfTime.DAY ? 86400000 : 604800000) / timeAmount;
		} else if (unitOfTime == UnitOfTime.MONTH) {
			Calendar currentCal = Calendar.getInstance();
			while (currentCal.get(Calendar.MONTH) != cal.get(Calendar.MONTH) || currentCal.get(Calendar.YEAR) != cal.get(Calendar.YEAR)) {
				currentCal.add(Calendar.MONTH, 1);
				numberOfTimes++;
			}
			numberOfTimes /= timeAmount;
		} else {
			Calendar currentCal = Calendar.getInstance();
			while (currentCal.get(Calendar.YEAR) != cal.get(Calendar.YEAR)) {
				currentCal.add(Calendar.YEAR, 1);
				numberOfTimes++;
			}
			numberOfTimes /= timeAmount;
		}
		return amt * numberOfTimes;
	}

	private int getComboBoxSelectedIndex(String colorStr) {
		int i = 0;
		for (String aColor: new String[]{"black", "blue", "cyan", "dark gray", "gray", "green", "light gray", "magenta", "orange", "pink", "red", "white", "yellow"}) {
			if (colorStr.equals(aColor)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	private int getUnitOfTimeSelectedIndex(UnitOfTime UOT) {
		int i = 0;
		for (UnitOfTime UOTChoice: new UnitOfTime[]{UnitOfTime.DAY, UnitOfTime.WEEK, UnitOfTime.MONTH, UnitOfTime.YEAR}) {
			if (UOT == UOTChoice) {
				return i;
			}
			i++;
		}
		return -1;
	}

	private String abbrev(String name) {
		String[] splitSpace = name.split(" ");
		if (splitSpace.length > 1) {
			String retStr = "";
			if (splitSpace[0].length() > 0) {
				String firstStr = splitSpace[0].substring(0, 1);
				char first = firstStr.charAt(0);
				if (firstStr.matches("[a-z]")) {
					first -= 32;
				}
				retStr += first;
			}
			if (splitSpace[1].length() > 0) {
				String secondStr = splitSpace[1].substring(0, 1);
				char second = secondStr.charAt(0);
				if (secondStr.matches("[a-z]")) {
					second -= 32;
				}
				retStr += second;
			}
			if (splitSpace.length > 2) {
				if (splitSpace[2].length() > 0) {
					String thirdStr = splitSpace[2].substring(0, 1);
					char third = thirdStr.charAt(0);
					if (thirdStr.matches("[a-z]")) {
						third -= 32;
					}
					retStr += third;
				}
			} else if (splitSpace[1].length() > 1) {
				retStr += splitSpace[1].charAt(1);
			}
			return retStr;
		} else {
			char[] caps = new char[3];
			int capsCtr = 0;
			for (int i = 0; i < name.length(); i++) {
				if (name.substring(i, i + 1).matches("[A-Z]|\\d")) {
					caps[capsCtr] = name.charAt(i);
					capsCtr++;
					if (capsCtr == 3) {
						return new String(caps);
					}
				}
			}
			if (name.length() > 3) {
				return name.substring(0, 3);
			}
			return name;
		}
	}

	private double getPercent(double amount, double total) {
		return roundCent(amount / total * 100);
	}

	private void updateFile() throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter("budget.txt"));
		writer.println("Total earnings:" + totalEarnings);
		writer.println("Total spendings:" + totalSpendings);
		writer.println("Color unused:" + colorUnusedStr);
		writer.println(":CATEGORIES");
		for (Category cat: categories) {
			writer.println(cat.name + ":" + cat.abbr + "," + cat.spending + "," + cat.colorStr + "," + cat.memo);
		}
		writer.println(":POSSIBLE SPENDINGS");
		for (SpecCategory sCat: specCategories) {
			writer.println(sCat.name + ":" + sCat.abbr + "," + sCat.spending + "," + sCat.colorStr + "," + (sCat.showInPie ? "show" : "hide"));
		}
		writer.println(":FORECAST CONFIG");
		writer.println("Go forward:" + projTimeAmount + "-" + projUnitOfTimeStr);
		writer.println("Factor in current spendings and earnings:" + factorInStr);
		writer.println("Earnings per unit time:" + forecastEarningsAmt + "," + forecastEarningsTimeAmount + "-" + forecastEarningsUnitOfTimeStr);
		for (ProjectionSetting projSett: projectionSettings) {
			writer.println(projSett.category.name + ":" + projSett.amount + "," + projSett.timeAmount + "-" + projSett.unitOfTimeStr);
		}
		writer.close();
	}

	private class Category {
		protected String name;
		protected String abbr;
		protected double spending;
		protected Color color; protected String colorStr;
		private String memo;
	}
	private class SpecCategory extends Category {
		private boolean showInPie;
	}

	private class ProjectionSetting {
		private Category category;
		private double amount = 0;
		private int timeAmount = 1;
		private UnitOfTime unitOfTime = UnitOfTime.MONTH; private String unitOfTimeStr = "month";
		private double calcAmt = 0;
	}

	private enum ViewOption {
		COMPLETE_BUDGET, UNUSED_MONEY, FORECAST
	}

	private enum UnitOfTime {
		DAY, WEEK, MONTH, YEAR
	}

}
