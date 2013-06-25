package com.report.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.FontKey;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.PdfFont;
import net.sf.jasperreports.engine.util.JRProperties;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import com.report.utils.ReportProperty;

public class ReportServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/**
	 * 
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String exportType = request.getParameter("report_type");
		String reportName = request.getParameter("report_name");
		Map parameterMap = buildParameter(request.getParameterMap());
		if (null == exportType) {
			return;
		}
		if (null == reportName) {
			return;
		}

		JasperPrint print = null;
		try {
			print = getReportPrint(
						ReportProperty.getInstance().get("database.url"), 
						ReportProperty.getInstance().get("database.username"), 
						ReportProperty.getInstance().get("database.password"), 
						ReportServlet.class.getResourceAsStream(reportName + ".jrxml"), parameterMap);
		} catch (SQLException e) {
			e.printStackTrace();
//			response.sendRedirect("error.jsp");
		}

		/** Generate Reprot by type **/
		if (exportType.equals("html")) {
			exportHtml(request, response, print, reportName);
		} else if (exportType.equals("pdf")) {
			exportPDF(request, response, print, reportName);
		} else if (exportType.equals("xls")) {
			exportExcel(request, response, print, reportName);
		} else if (exportType.equals("rtf")) {
			exportRTF(request, response, print, reportName);
		}
	}

	private Map<String, String> buildParameter(Map<String, String[]> parameters) {
		Map<String, String> m = new HashMap<String, String>();
		for (Iterator<String> iterator = parameters.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			String value = ((String[]) parameters.get(key))[0];
			System.out.print( "Key : " + key );
			System.out.println( " =>Value : " + value );
			m.put(key, decoderParam( value.replaceAll("\\*", "%")));
			// System.out.println(decoderParam(
			// ((String[]) parameters.get(key))[0]).replaceAll("\\*",
			// "%"));

		}
		return m.size() != 0 ? m : null;
	}

	private String decoderParam(String param) {
		Charset encodeCharset = Charset.forName("ISO-8859-1");
		Charset decodeCharset = Charset.forName("UTF8");
		CharsetDecoder decoder = decodeCharset.newDecoder();
		CharsetEncoder encoder = encodeCharset.newEncoder();

		try {
			// Convert a string to ISO-LATIN-1 bytes in a ByteBuffer
			// The new ByteBuffer is ready to be read.
			ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(param));

			// Convert ISO-LATIN-1 bytes in a ByteBuffer to a character
			// ByteBuffer and then to a string.
			// The new ByteBuffer is ready to be read.
			CharBuffer cbuf = decoder.decode(bbuf);
			return cbuf.toString();
		} catch (CharacterCodingException e) {
			e.printStackTrace();
		}
		return param;
	}

	protected void exportHtml(HttpServletRequest request, HttpServletResponse response, JasperPrint print, String reportName) {
		try {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			JRHtmlExporter exporter = new JRHtmlExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
			exporter.setParameter(JRExporterParameter.OUTPUT_WRITER, out);
			exporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "UTF-8");
			exporter.exportReport();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				response.sendRedirect("error.jsp");
			} catch (Exception ex) {
				ex.printStackTrace();

			}
		}
	}

	protected void exportRTF(HttpServletRequest request, HttpServletResponse response, JasperPrint print, String fileName) {
		try {
			/* 4 open/Save report in XLS format */			
			JRRtfExporter rtfExporter = new JRRtfExporter();
			rtfExporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
			rtfExporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, response.getOutputStream());
			rtfExporter.setParameter(JRXlsExporterParameter.CHARACTER_ENCODING, "UTF-8");
			response.reset();
			response.setHeader("Content-type", "application/rtf");
			response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".rtf");
			rtfExporter.exportReport();
		} catch (JRException jre) {
			jre.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void exportExcel(HttpServletRequest request, HttpServletResponse response, JasperPrint print, String fileName) {
		try {
			/* 4 open/Save report in XLS format */			
			JRXlsExporter xlsExporter = new JRXlsExporter();
			xlsExporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
			xlsExporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, response.getOutputStream());
			xlsExporter.setParameter(JRXlsExporterParameter.CHARACTER_ENCODING, "UTF-8");
			response.reset();
			response.setHeader("Content-type", "application/xls");
			response.setHeader("Content-disposition", "attachment; filename=" + fileName + ".xls");
			xlsExporter.exportReport();
		} catch (JRException jre) {
			jre.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	protected void exportPDF(HttpServletRequest request, HttpServletResponse response, JasperPrint print, String reportName) {
		try {			
			JRPdfExporter pdfExporter = new JRPdfExporter();					
			pdfExporter.setParameter(JRPdfExporterParameter.JASPER_PRINT, print);
			pdfExporter.setParameter(JRPdfExporterParameter.OUTPUT_STREAM, response.getOutputStream());
			pdfExporter.setParameter(JRPdfExporterParameter.CHARACTER_ENCODING, "UTF-8");
			
			response.reset();
			response.setHeader("Content-type", "application/pdf");
			response.setHeader("Content-disposition", "attachment; filename=" + reportName + ".pdf");
			pdfExporter.exportReport();			
		} catch (Exception e) {
			e.printStackTrace();
//			try {
//				response.sendRedirect("error.jsp");
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
		}
	}

	private JasperPrint getReportPrint(String databaseName, String userName, String password, InputStream inputStream, Map parameters) throws SQLException {
		JasperPrint jasperPrint = null;
		Connection jdbcConnection = null;
		try {
			JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
			jdbcConnection = connectDB(databaseName, userName, password);
			jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jdbcConnection);
		} catch (Exception ex) {
			String connectMsg = "Could not create the report stream " + ex.getMessage() + " " + ex.getLocalizedMessage();
			System.out.println(connectMsg);
		} finally {
			if (null != jdbcConnection) {
				jdbcConnection.close();
			}
		}
		return jasperPrint;
	}

	private Connection connectDB(String databaseName, String userName, String password) {
		Connection jdbcConnection = null;
		try {
			Class.forName(ReportProperty.getInstance().get("datasource.driver"));
			jdbcConnection = DriverManager.getConnection(databaseName, userName, password);
		} catch (Exception ex) {
			String connectMsg = "Could not connect to the database: " + ex.getMessage() + " " + ex.getLocalizedMessage();
			System.out.println(connectMsg);
		}
		return jdbcConnection;
	}
}