<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version='1.0'>

	<xsl:import
	href="http://docbook.sourceforge.net/release/xsl/current/html/docbook.xsl"/>
	<!--
	<xsl:import
	href="file:///Users/mprudhom/cvs/31/solarmetric/doc/styles/docbook/docbook-xsl/html/docbook.xsl"/>
	-->

	<xsl:param name="html.stylesheet">styles.css</xsl:param>
	<xsl:param name="html.cleanup" select="1"/>
	<xsl:param name="label.from.part">1</xsl:param>
	<xsl:param name="annotate.toc" select="1"/>
	<xsl:param name="toc.section.depth">5</xsl:param>
	<!--
	<xsl:param name="generate.section.toc.level" select="8"/>
	-->

	<!--
	<xsl:param name="generate.book.toc" select="1"/>
	<xsl:param name="generate.component.toc" select="1"/>
	<xsl:param name="generate.division.toc" select="1"/>
	-->

	<xsl:param name="generate.index" select="1"/>
	<xsl:param name="chapter.autolabel" select="0"/>
	<xsl:param name="appendix.autolabel" select="0"/>
	<xsl:param name="part.autolabel" select="0"/>
	<xsl:param name="preface.autolabel" select="0"/>
	<xsl:param name="qandadiv.autolabel" select="1"/>
	<xsl:param name="section.autolabel" select="0"/>
	<xsl:param name="section.label.includes.component.label" select="1"/>

	<!--
	<xsl:param name="admon.graphics" select="1"/>
	<xsl:param name="admon.graphics.path">img/</xsl:param>
	<xsl:param name="admon.graphics.extension" select="'.gif'"/>

	<xsl:param name="label.from.part" select="1"/>
	-->

	<xsl:param name="generate.toc">
		appendix  toc    
		article   toc    
		book      toc,fig
		chapter   toc    
		part      toc    
		preface   toc    
		qandadiv  toc    
		qandaset  toc    
		reference toc    
		refentry  toc    
		section   toc    
		set       toc    
	</xsl:param>     
</xsl:stylesheet>

