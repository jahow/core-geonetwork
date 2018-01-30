<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:geonet="http://www.fao.org/geonetwork">
  <xsl:output method="text"/>

  <xsl:include href="../../common/base-variables.xsl"/>

  <xsl:template match="/">
    <!-- <xsl:text>sitemap: </xsl:text><xsl:value-of select="concat($fullURLForService, '/portal.sitemap')"/> -->
    <xsl:text>sitemap: </xsl:text><xsl:value-of select="concat($fullURLForService, '/portal.sitemap?format=pigma_seo')"/>
  </xsl:template>
</xsl:stylesheet>
