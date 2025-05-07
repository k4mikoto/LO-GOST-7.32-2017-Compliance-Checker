package org.libreoffice.example.dialog;

import org.libreoffice.example.helper.DialogHelper;
import org.libreoffice.example.helper.DocumentHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XText;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.style.LineSpacing;

import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XPageCursor;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.style.*;

import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XButton;

import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogEventHandler;
import com.sun.star.frame.XFrame;
import com.sun.star.awt.XTextComponent;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.XComponentContext;


public class ActionOneDialog implements XDialogEventHandler {
	
	private XDialog dialog;
	private static final String OnCheckButtonPress = "OnCheckButtonPress";
	private static final String OnSaveBtnPress = "OnSaveBtnPress";
	private String[] supportedActions = new String[] { OnCheckButtonPress , OnSaveBtnPress, };
	short RadioState = 1;
	private String stringa = "";
	private XComponentContext internalContext;
	private XTextDocument document;
	
	@SuppressWarnings("unused")
	private class GOST {
		//public static final String ;
		public static final String Font = "Times New Roman";
		public static final float Height = 14.0f;
		public static final long Indent = 1250;
		public static final float Weight = 100.0f;
		public static final short Adjust = 2;
		public static final long HeadingIndent = 0;
		public static final float HeadingWeight = 150.0f;
		public static final short HeadingAdjust = 3;
		public static final short SpacingHeight = 150;
		public static final short SpacingMode = 0;
		public static final long MarginSum = 0;
		public class PageMargins{
			public static final long LeftMargin = 3000;
			public static final long RightMargin = 1500;
			public static final long TopMargin = 2000;
			public static final long BottomMargin = 2000;
		}
		//Left 0 Right 1 Just 2 Center 3
	}
	
	public ActionOneDialog(XComponentContext xContext) {
		this.dialog = DialogHelper.createDialog("ActionOneDialog.xdl", xContext, this);
		//rip out the context for future use
		internalContext = xContext;
		
		//init label
		XTextComponent label = DialogHelper.getEditField(dialog, "DocNameField");
		document = DocumentHelper.getCurrentDocument(internalContext);
		String fileURL = "";
		try {
			fileURL = java.net.URLDecoder.decode(document.getURL(), StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		label.setText(fileURL.substring(8));
	}
	
	public void show() {
		dialog.execute();
	}
	
	private void theMethodFinal() {
		//disable the button
		DialogHelper.EnableButton(dialog, "CheckButton", false);
		DialogHelper.SetFocus(DialogHelper.getEditField(dialog, "LogTextbox"));
		
		//hashset for looking up if this page's info was printed out
		HashSet<Short> UsedPages = new HashSet<Short>();
		
		//get the text
		XTextRange textitself = document.getText();
		
		//get page style family for page properties checks
		XStyleFamiliesSupplier styleSupplier = UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, document);
		XNameAccess StyleFamilies = styleSupplier.getStyleFamilies();
		Object StyleFamily = null;
		try {
			StyleFamily = StyleFamilies.getByName("PageStyles");
		} catch (NoSuchElementException | WrappedTargetException e) {
			e.printStackTrace();
		}
		XNameContainer pageStyleFamily = UnoRuntime.queryInterface(XNameContainer.class, StyleFamily);
		
		//init view and page cursors for page number extraction
		XTextViewCursor viewCursor = DocumentHelper.getViewCursor(internalContext);
		XPageCursor pageCursor = UnoRuntime.queryInterface(XPageCursor.class, viewCursor);
		
		//init access to paragraph enumeration
		XEnumerationAccess ParaAccess = UnoRuntime.queryInterface(XEnumerationAccess.class, textitself);
		XEnumeration ParaEnum = ParaAccess.createEnumeration();
		int paracount = 1;
		short contentsPage = 0;
		//Style data extraction by paragraph
		while (ParaEnum.hasMoreElements()){
			try {
				Object CurrentPara = ParaEnum.nextElement();
				XServiceInfo ParaInfo = UnoRuntime.queryInterface(XServiceInfo.class, CurrentPara);
				if (!ParaInfo.supportsService("com.sun.star.text.TextTable")) {
	                // accesses com.sun.star.style.ParagraphProperties
	                XPropertySet ParaSet = UnoRuntime.queryInterface(XPropertySet.class, ParaInfo);
	                
	                String PageStyleName = "";
	                PageStyleName += ParaSet.getPropertyValue("PageStyleName");
	                Object PageStyle = pageStyleFamily.getByName(PageStyleName);
	                XStyle CurrentPageStyle = UnoRuntime.queryInterface(XStyle.class, PageStyle);
	                XPropertySet PageSet = UnoRuntime.queryInterface(XPropertySet.class, CurrentPageStyle);
	                
	                //store all info obtainable from current paragraph
	            	long CurrentParaIndent = Long.valueOf(ParaSet.getPropertyValue("ParaFirstLineIndent").toString());
	            	short CurrentParaAdjust = Short.valueOf(ParaSet.getPropertyValue("ParaAdjust").toString());
	            	LineSpacing CurrentParaSpacing = (LineSpacing) ParaSet.getPropertyValue("ParaLineSpacing");
	            	long CurrentParaMarginSum = Long.valueOf(ParaSet.getPropertyValue("ParaLeftMargin").toString()) + 
	            			Long.valueOf(ParaSet.getPropertyValue("ParaRightMargin").toString()) + 
	            			Long.valueOf(ParaSet.getPropertyValue("ParaTopMargin").toString()) + 
	            			Long.valueOf(ParaSet.getPropertyValue("ParaBottomMargin").toString());
	            	long CurrentPageBorderDistanceSum = Long.valueOf(PageSet.getPropertyValue("LeftBorderDistance").toString()) + 
	            			Long.valueOf(PageSet.getPropertyValue("RightBorderDistance").toString()) + 
	            			Long.valueOf(PageSet.getPropertyValue("TopBorderDistance").toString()) + 
	            			Long.valueOf(PageSet.getPropertyValue("BottomBorderDistance").toString());
	            	long CurrentPageLeftMargin = Long.valueOf(PageSet.getPropertyValue("LeftMargin").toString());
	            	long CurrentPageRightMargin = Long.valueOf(PageSet.getPropertyValue("RightMargin").toString());
	            	long CurrentPageTopMargin = Long.valueOf(PageSet.getPropertyValue("TopMargin").toString());
	            	long CurrentPageBottomMargin = Long.valueOf(PageSet.getPropertyValue("BottomMargin").toString());
					
					//get the info on text portions from current paragraph
					XEnumerationAccess CurrentParaAccess = UnoRuntime.queryInterface(XEnumerationAccess.class, CurrentPara);
					XEnumeration PortionEnum = CurrentParaAccess.createEnumeration();
					
					boolean skipPage = false;
					boolean hasHeadings = false;
					boolean raised = false;
					boolean weightRaised = false;
					boolean fontRaised = false;
					String portionMistakes = "";
					short paraPage = 0;
					String portionText = "";
					while (PortionEnum.hasMoreElements()) {
						Object CurrentPortion = PortionEnum.nextElement();
						
						//move the cursors and get the page number
						XServiceInfo PortionInfo = UnoRuntime.queryInterface(XServiceInfo.class, CurrentPortion);
						XPropertySet PortionSet = UnoRuntime.queryInterface(XPropertySet.class, PortionInfo);
						XTextRange CurrentText = UnoRuntime.queryInterface(XTextRange.class, CurrentPortion);
						
						viewCursor.gotoRange(CurrentText, false);;
						pageCursor = UnoRuntime.queryInterface(XPageCursor.class, viewCursor);
						
						//store all info obtainable from current portion, 1st page is excluded from checking process
						short CurrentPortionPage = pageCursor.getPage();
						if ((CurrentPortionPage == 1)|| (CurrentPortionPage == contentsPage)){
							skipPage = true;
							break;
						}
						paraPage = pageCursor.getPage();
						String CurrentPortionType = PortionSet.getPropertyValue("TextPortionType").toString();
						if (CurrentPortionType.equals("Bookmark")) hasHeadings = true;
						String CurrentPortionFont = PortionSet.getPropertyValue("CharFontName").toString();
						float CurrentPortionWeight = Float.valueOf(PortionSet.getPropertyValue("CharWeight").toString());
						float CurrentPortionHeight = Float.valueOf(PortionSet.getPropertyValue("CharHeight").toString());
						String CurrentPortionText = CurrentText.getString();
						portionText += CurrentPortionText;
						//textportion checking process
						String currentPortionMistakes = "";
						//begins with checking page parameters. if the page was already checked it won't be checked again
						//HashSet.add() returns true if added and false if element already exists which makes a perfect if condition
						if (UsedPages.add(CurrentPortionPage)) {
							String pageMistakes = "";
							raised = false;
							//page margins
							if (!between(CurrentPageLeftMargin, GOST.PageMargins.LeftMargin)) {
								pageMistakes += "Левое поле: " + ((float)CurrentPageLeftMargin/1000) + "см, ожидалось: 3см\n";
								raised = true;
							}
							if (!between(CurrentPageRightMargin, GOST.PageMargins.RightMargin)) {
								pageMistakes += "Правое поле: " + ((float)CurrentPageRightMargin/1000) + "см, ожидалось: 1.5см\n";
								raised = true;
							}
							if (!between(CurrentPageTopMargin, GOST.PageMargins.TopMargin)) {
								pageMistakes += "Верхнее поле: " + ((float)CurrentPageTopMargin/1000) + "см, ожидалось: 2см\n";
								raised = true;
							}
							if (!between(CurrentPageBottomMargin, GOST.PageMargins.BottomMargin)) {
								pageMistakes += "Нижнее поле: " + ((float)CurrentPageBottomMargin/1000) + "см, ожидалось: 2см\n";
								raised = true;
							}
							//page border distance
							if (CurrentPageBorderDistanceSum>0) {
								pageMistakes += "На странице не должно быть рамок\n";
								raised = true;
							}
							currentPortionMistakes += pageMistakes;
						}
						//font checking process
						if (!(CurrentPortionFont.contentEquals(GOST.Font))&&!fontRaised) {
							currentPortionMistakes += "Шрифт: " + CurrentPortionFont + ", ожидается: " + GOST.Font + "\n";
							raised = true;
							fontRaised = true;
						}
						if (!(CurrentPortionHeight == GOST.Height)) {
							currentPortionMistakes += "Размер Шрифта: " + CurrentPortionHeight + ", ожидается: " + GOST.Height + "\n";
							raised = true;
						}
						if (!weightRaised) {
							if (hasHeadings) {
								if (!(CurrentPortionWeight == GOST.HeadingWeight)&&(!CurrentPortionText.equals(""))) {
									int dotcount = portionText.length() - portionText.replaceAll(".","").length();
									if (dotcount < 2) {
										currentPortionMistakes += "Заголовки разделов и структурных элементов должны быть написаны полужирным шрифтом\n" +CurrentPortionText + "\n";
										raised = true;
										weightRaised = true;
									}
								}
							} else {
								if (!(CurrentPortionWeight == GOST.Weight)) {
									if (!portionText.contains("РЕФЕРАТ")) {
										currentPortionMistakes += "Полужирный шрифт допустимо применять только для заголовков разделов и подразделов, заголовков структурных элементов\n";
										raised = true;
										weightRaised = true;
									}
								}
							}
						}
						portionMistakes += currentPortionMistakes;
					//end of portion cycle
					}
					//para checking process
					//enforcing not checking the 1st page rule on paragraph scale

					if (portionText.equals("СОДЕРЖАНИЕ")) {
						skipPage = true;
						contentsPage = paraPage;
					}
					if (skipPage) {
						continue;
					}
					

					boolean imgflag = portionText.contains("Рисунок");
					boolean referatflag = portionText.contains("РЕФЕРАТ");
					
					String paraMistakes = "Параграф: " + paracount + " | Страница: " + paraPage + "\n" + "Текст: " + portionText +"\n";
					if (!portionText.equals("")){
						//if it isn't a blank paragraph
						if (hasHeadings) {
							//if it has a bookmark
							if (portionText.equals("ВВЕДЕНИЕ")||portionText.equals("ЗАКЛЮЧЕНИЕ")||portionText.equals("СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ")) {
								//if it's a special heading
								if (!(CurrentParaAdjust == GOST.HeadingAdjust)) {
									//if it isn't centered
									String adjustVal = "";
					                adjustVal += CurrentParaAdjust;
									switch (adjustVal){
										case ("0"): adjustVal = "по левому краю"; break;
										case ("1"): adjustVal = "по правому краю"; break;
										case ("2"): adjustVal = "по ширине"; break;
										case ("3"): adjustVal = "по центру"; break;
										default:  adjustVal = "Undefined"; break;
									}
									paraMistakes += "Выравнивание: " + adjustVal + ", для данных заголовков ожидается выравнивание по центру\n";
									raised = true;
								}
								if (CurrentParaIndent != GOST.HeadingIndent) {
									//if it has indent
									paraMistakes += "Абзацный отступ: " + ((float)CurrentParaIndent/1000) + "см, для данных заголовков ожидается: " + (float)GOST.HeadingIndent + "см\n";
									raised = true;
								}
							} else {
								//if it isn't a special heading
								if (!(CurrentParaAdjust == GOST.Adjust)) {
									//if it isn't justified
									String adjustVal = "";
					                adjustVal += CurrentParaAdjust;
									switch (adjustVal){
										case ("0"): adjustVal = "по левому краю"; break;
										case ("1"): adjustVal = "по правому краю"; break;
										case ("2"): adjustVal = "по ширине"; break;
										case ("3"): adjustVal = "по центру"; break;
										default:  adjustVal = "Undefined"; break;
									}
									paraMistakes += "Выравнивание: " + adjustVal + ", для данных заголовков ожидается выравнивание по ширине\n";
									raised = true;
								}
								if (!between(CurrentParaIndent, GOST.Indent)) {
									//if it doesn't have an indent
									paraMistakes += "Абзацный отступ: " + ((float)CurrentParaIndent/1000) + "см, для данных заголовков ожидается: " + (float)GOST.HeadingIndent + "см\n";
									raised = true;
								}
							}
						} else {
							//if it doesn't contain a bookmark
							if (!between(CurrentParaIndent, GOST.Indent)&&!referatflag) {
								//if it doesn't have an indent
								paraMistakes += "Абзацный отступ: " + ((float)CurrentParaIndent/1000) + "см,  ожидается: " + (float)GOST.Indent/1000 + "см\n";
								raised = true;
							}
							if (!(CurrentParaAdjust == GOST.Adjust)) {
								//if it isn't justified
								if (!imgflag&&!referatflag) {
									//if it isn't an image description or a special heading that isn't a bookmark
									String adjustVal = "";
					                adjustVal += CurrentParaAdjust;
									switch (adjustVal){
										case ("0"): adjustVal = "по левому краю"; break;
										case ("1"): adjustVal = "по правому краю"; break;
										case ("2"): adjustVal = "по ширине"; break;
										case ("3"): adjustVal = "по центру"; break;
										default:  adjustVal = "Undefined"; break;
									}
									paraMistakes += "Выравнивание: " + adjustVal + ", для текста ожидается выравнивание по ширине\n";
									raised = true;
								} else {
									//if it is an image description or a special heading that isn't a bookmark
									if (CurrentParaAdjust != 3) {
										//if it's not centered
										String adjustVal = "";
						                adjustVal += CurrentParaAdjust;
										switch (adjustVal){
											case ("0"): adjustVal = "по левому краю"; break;
											case ("1"): adjustVal = "по правому краю"; break;
											case ("2"): adjustVal = "по ширине"; break;
											case ("3"): adjustVal = "по центру"; break;
											default:  adjustVal = "Undefined"; break;
										}
										paraMistakes += "Выравнивание: " + adjustVal + ", ожидается выравнивание по центру\n";
										raised = true;
									}
								}
							}
						}
						//if the spacing ain't right
						if (CurrentParaSpacing.Height != GOST.SpacingHeight) {
							paraMistakes += "Межстрочный интервал: " + (float)CurrentParaSpacing.Height/100 + ", ожидается 1.5\n";
							raised = true;
						}
						if (CurrentParaMarginSum != GOST.MarginSum) {
							paraMistakes += "Отступы и интервалы в тексте недопустимы\n";
							raised = true;
						}
					}
					else {
						paraMistakes += "В тексте не должно быть пустых строк\n";
						raised = true;
					}
					paraMistakes += portionMistakes;
					
					//if a mistake flag was raised everything goes to output
					if(raised) {
						stringa += paraMistakes + "\n";
					}
	            }
			} catch (NoSuchElementException | WrappedTargetException | UnknownPropertyException e) {
				e.printStackTrace();
			}
			paracount++;
			//end of paragraph cycle
		}
		
		
		//end of algorithm;
		if(!stringa.equals("")) {
			printout(stringa);
			DialogHelper.EnableButton(dialog, "SaveButton", true);
		}else {
			stringa += "В данном документе не обнаружено ошибок форматирования";
			printout(stringa);
		}
	}
	
	private void theMethod() {
		//disable the button
		DialogHelper.EnableButton(dialog, "CheckButton", false);
		DialogHelper.SetFocus(DialogHelper.getEditField(dialog, "LogTextbox"));
		
		//get the text
		XTextRange textitself = document.getText();
		
		//get page style family for page properties checks
		XStyleFamiliesSupplier styleSupplier = UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, document);
		XNameAccess StyleFamilies = styleSupplier.getStyleFamilies();
		Object StyleFamily = null;
		try {
			StyleFamily = StyleFamilies.getByName("PageStyles");
		} catch (NoSuchElementException | WrappedTargetException e) {
			e.printStackTrace();
		}
		XNameContainer pageStyleFamily = UnoRuntime.queryInterface(XNameContainer.class, StyleFamily);
		
		//init view and page cursors for page number extraction
		XTextViewCursor viewCursor = DocumentHelper.getViewCursor(internalContext);
		XPageCursor pageCursor = UnoRuntime.queryInterface(XPageCursor.class, viewCursor);
		
		//init access to paragraph enumeration
		XEnumerationAccess ParaAccess = UnoRuntime.queryInterface(XEnumerationAccess.class, textitself);
		XEnumeration ParaEnum = ParaAccess.createEnumeration();
		int paracount = 1;
		//Style data extraction by paragraph
		while (ParaEnum.hasMoreElements()){
			try {
				Object CurrentPara = ParaEnum.nextElement();
				XServiceInfo ParaInfo = UnoRuntime.queryInterface(XServiceInfo.class, CurrentPara);
				if (!ParaInfo.supportsService("com.sun.star.text.TextTable")) {
	                // accesses com.sun.star.style.ParagraphProperties
	                XPropertySet ParaSet = UnoRuntime.queryInterface(XPropertySet.class, ParaInfo);
	                
	                String PageStyleName = "";
	                PageStyleName += ParaSet.getPropertyValue("PageStyleName");
	                Object PageStyle = pageStyleFamily.getByName(PageStyleName);
	                XStyle CurrentPageStyle = UnoRuntime.queryInterface(XStyle.class, PageStyle);
	                XPropertySet PageSet = UnoRuntime.queryInterface(XPropertySet.class, CurrentPageStyle);
	                
	                //get the adjustment
	                String adjustVal = "";
	                adjustVal += ParaSet.getPropertyValue("ParaAdjust");
					switch (adjustVal){
						case ("0"): adjustVal = "Left"; break;
						case ("1"): adjustVal = "Right"; break;
						case ("2"): adjustVal = "Justified"; break;
						case ("3"): adjustVal = "Center"; break;
						default:  adjustVal = "Undefined"; break;
					}
					
					//get the line spacing
					LineSpacing CurrentParaSpacing = (LineSpacing) ParaSet.getPropertyValue("ParaLineSpacing");
					
					//debug print all info accessible from paragraph
					stringa = stringa + "Para: " + paracount + 
							"\n" +
							"PageStyle: " + ParaSet.getPropertyValue("PageStyleName") + 
							" | PageMargins(LRTB): " + PageSet.getPropertyValue("LeftMargin") + " " +
							PageSet.getPropertyValue("RightMargin") + " " +
							PageSet.getPropertyValue("TopMargin") + " " +
							PageSet.getPropertyValue("BottomMargin") +
							" | PageBorderDistance(LRTB): " + PageSet.getPropertyValue("LeftBorderDistance") + " " +
							PageSet.getPropertyValue("RightBorderDistance") + " " +
							PageSet.getPropertyValue("TopBorderDistance") + " " +
							PageSet.getPropertyValue("BottomBorderDistance") +
							"\n"+
							"Style: " + ParaSet.getPropertyValue("ParaStyleName") + 
							" | Firstline: " + ParaSet.getPropertyValue("ParaFirstLineIndent") + 
							" | Adjust: " + adjustVal +
							" | Spacing(H|M): " + CurrentParaSpacing.Height + " " + CurrentParaSpacing.Mode +
							" | Margin(LRTB): " + ParaSet.getPropertyValue("ParaLeftMargin") + " " +
							ParaSet.getPropertyValue("ParaRightMargin") + " " +
							ParaSet.getPropertyValue("ParaTopMargin") + " " +
							ParaSet.getPropertyValue("ParaBottomMargin") +
							//" | LineSpacing: " + ParaSet.getPropertyValue("ParaLineSpacing") +
							"\n";	
					
					//get the info on text portions from current paragraph
					XEnumerationAccess CurrentParaAccess = UnoRuntime.queryInterface(XEnumerationAccess.class, CurrentPara);
					XEnumeration PortionEnum = CurrentParaAccess.createEnumeration();
					while (PortionEnum.hasMoreElements()) {
						Object CurrentPortion = PortionEnum.nextElement();
						
						//move the cursors and get the page number
						XServiceInfo PortionInfo = UnoRuntime.queryInterface(XServiceInfo.class, CurrentPortion);
						XPropertySet PortionSet = UnoRuntime.queryInterface(XPropertySet.class, PortionInfo);
						XTextRange CurrentText = UnoRuntime.queryInterface(XTextRange.class, CurrentPortion);
						
						viewCursor.gotoRange(CurrentText, false);;
						pageCursor = UnoRuntime.queryInterface(XPageCursor.class, viewCursor);
						//debug print all info from current text portion
						stringa = stringa + "Type: " + PortionSet.getPropertyValue("TextPortionType") + 
								" | Page: " + pageCursor.getPage() + 
								" | Font: " + PortionSet.getPropertyValue("CharFontName") + 
								" | Weight: " + PortionSet.getPropertyValue("CharWeight") + 
								" | Height: " + PortionSet.getPropertyValue("CharHeight") + 
								" | Contents: " + CurrentText.getString() + 
								"\n";
					}
					stringa += "\n";
	            }
			} catch (NoSuchElementException | WrappedTargetException | UnknownPropertyException e) {
				e.printStackTrace();
			}
			paracount++;
			//end of 1 cycle
		}
		
		
		//end of algorithm
		printout(stringa);
		DialogHelper.EnableButton(dialog, "SaveButton", true);
	}
	
	private void onCheckButtonPress() {
		//get left radio button state and disable them
		XPropertySet Radio1 = DialogHelper.getRadioButtonInfo(dialog, "Choice1");
		XPropertySet Radio2 = DialogHelper.getRadioButtonInfo(dialog, "Choice2");
		try {
			Radio1.setPropertyValue("Enabled", false);
			Radio2.setPropertyValue("Enabled", false);
			RadioState = (short) Radio1.getPropertyValue("State");
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			e.printStackTrace();
		}
		
		// 1 = check; 0 = debug
		if (RadioState == 1) {
			theMethodFinal();
		}
		else {
			theMethod();
		}
	}
	
	private void onOkButtonPressed() {
		dialog.endExecute();
	}
	
	private void onSaveButtonPressed() throws URISyntaxException {
		//creating the file path
		String statemarker = "";
		if (RadioState == 1) statemarker = "check";
		else statemarker = "debug";
		String fileURL = document.getURL();
		String logFilePath = fileURL.replace(" ", "_") + "." + statemarker + ".log.txt";
		URI uri = new URI(logFilePath);
		Path savepath = Paths.get(uri);
		
		//saving the file
		List<String> lines = Arrays.asList(stringa);
		try {
			Files.write(savepath, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//exit the program
		onOkButtonPressed();
	}
	
    public boolean between(Long Val, Long Ref) {
    	if (((Ref +1)>= Val) && (Val >= (Ref-1))) return true;
    	else return false;
    }
	
	private void printout(String text) {
		XTextComponent printer = DialogHelper.getEditField(dialog, "LogTextbox");
		printer.setText(text);
	}
	
	@Override
	public boolean callHandlerMethod(XDialog dialog, Object eventObject, String methodName) throws WrappedTargetException {
		if (methodName.equals(OnCheckButtonPress)) {
			onCheckButtonPress();
			return true; // Event was handled
		}
		if (methodName.equals(OnSaveBtnPress)) {
			try {
				onSaveButtonPressed();
			} catch (URISyntaxException e) {
				DialogHelper.showErrorMessage(internalContext, dialog, "Сохранение не удалось.");
				e.printStackTrace();
			}
			return true; // Event was handled
		}
		return false; // Event was not handled
	}

	@Override
	public String[] getSupportedMethodNames() {
		return supportedActions;
	}

}
