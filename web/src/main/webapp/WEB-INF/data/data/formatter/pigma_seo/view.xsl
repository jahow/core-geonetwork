<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmx="http://www.isotc211.org/2005/gmx" xmlns:srv="http://www.isotc211.org/2005/srv" xmlns:gml="http://www.opengis.net/gml" xmlns:gts="http://www.isotc211.org/2005/gts" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
  <!-- A simple XSL stylesheet to create a simple HTML view aimed to be indexed by search engines -->
  <xsl:variable name="md" select="/root/gmd:MD_Metadata"/>
  <xsl:variable name="mdTitle" select="$md/gmd:identificationInfo//gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString/text()"/>
  <xsl:variable name="mdContactName" select="$md/gmd:identificationInfo//gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString/text()"/>
  <xsl:variable name="contactMailAddress" select="$md/gmd:identificationInfo//gmd:citation/gmd:CI_Citation/gmd:citedResponsibleParty/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:address/gmd:CI_Address/gmd:electronicMailAddress/gco:CharacterString/text()"/>
  <xsl:variable name="mdOrgName" select="$md/gmd:identificationInfo//gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName/gco:CharacterString/text()"/>
  <xsl:variable name="mdUuid" select="$md/gmd:fileIdentifier/gco:CharacterString"/>
  <xsl:variable name="mdAbstract" select="$md/gmd:identificationInfo//gmd:abstract/gco:CharacterString/text()"/>
  <xsl:variable name="mdLimitations" select="$md/gmd:identificationInfo//gmd:resourceConstraints/gmd:MD_Constraints/gmd:useLimitation/gco:CharacterString/text()"/>
  <xsl:variable name="mdKeywords" select="$md/gmd:identificationInfo//gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString/text()"/>
  <xsl:variable name="orgLogo" select="$md/gmd:identificationInfo//gmd:pointOfContact[1]/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:contactInstructions/gmx:FileName/@src"/>
  <xsl:variable name="orgWebsite" select="string-join($md/gmd:identificationInfo//gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:contactInfo/gmd:CI_Contact/gmd:onlineResource/gmd:CI_OnlineResource/gmd:linkage/gmd:URL/text(), '')" />
  <xsl:variable name="graphicOverview" select="$md/gmd:identificationInfo[1]//gmd:graphicOverview/gmd:MD_BrowseGraphic[1]/gmd:fileName/gco:CharacterString/text()"/>
  <xsl:variable name="catalogUrl" select="'/geonetwork'"/>
  <xsl:variable name="mdUrl" select="string-join(($catalogUrl, '/srv/fre/catalog.search#/metadata/', $mdUuid), '')"/>
  <xsl:variable name="mfappUrl" select="'/mapfishapp/'"/>
  <xsl:variable name="thumbnailUrl">
    <xsl:if test="not(starts-with($graphicOverview, 'http'))">
      /geonetwork/srv/fre/resources.get?fname=<xsl:value-of select="$graphicOverview"/>&amp;uuid=<xsl:value-of select="$mdUuid"/>
    </xsl:if>
    <xsl:if test="starts-with($graphicOverview, 'http')">
      <xsl:value-of select="$graphicOverview"/>
    </xsl:if>
  </xsl:variable>

  <xsl:template match="gmd:identificationInfo//gmd:citation//gmd:date">
    <xsl:variable name="dateType" select="./gmd:CI_Date/gmd:dateType/gmd:CI_DateTypeCode/@codeListValue" />
    <xsl:variable name="dateValue" select="./gmd:CI_Date/gmd:date/gco:Date/text()" />
    <xsl:if test="$dateValue != ''">
      <dl class="dl-horizontal">
        <dt>Date</dt>
        <dd>
          <xsl:value-of select="$dateValue"/>
          <xsl:if test="$dateType != ''"> (<xsl:value-of select="$dateType" />)</xsl:if>
        </dd>
      </dl>
    </xsl:if>
  </xsl:template>

  <xsl:template match="/">
    <html xmlns:og="http://ogp.me/ns#">
      <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
        <xsl:if test="$mdTitle != ''">
          <title><xsl:value-of select="$mdTitle"/></title>
        </xsl:if>
        <xsl:if test="$mdKeywords != ''">
          <xsl:variable name="keywordStr" select="string-join($mdKeywords, ', ')" />
          <meta name="keywords" content="{$keywordStr}" />
        </xsl:if>
        <meta name="description" content="{$mdAbstract}" />
        <link rel="canonical" href="{$mdUrl}" />
        <link rel="image_src" href="{$thumbnailUrl}" />
        <meta property="og:title" content="{$mdTitle}" />
        <meta property="og:type" content="article" />
        <meta property="og:url" content="{$mdUrl}" />
        <meta property="og:image" content="{$thumbnailUrl}" />
      </head>
      <body>
        <div class="container">
          <div class="row vert-space">
            <a href="/">
              <img src="/img/bandeau-pigma.png" alt="bandeau pigma" />
            </a>
          </div>
        </div>
        <div class="container metadata-main">
          <div class="row">
            <div class="col-md-12">
              <h1 class="metadata-title">
                <xsl:value-of select="$mdTitle"/>
              </h1>
            </div>
          </div>
          <div class="row vert-space text-justify">
            <!--  partie gauche -->
            <div class="col-md-9 metadata-content">
              <dl class="dl-horizontal">
                <dt>Identifiant</dt>
                <dd>
                  <xsl:value-of select="$mdUuid"/>
                </dd>
              </dl>
              <dl class="dl-horizontal">
                <dt>Résumé</dt>
                <dd>
                  <xsl:value-of select="$mdAbstract"/>
                </dd>
              </dl>
              <xsl:apply-templates select="$md//gmd:identificationInfo//gmd:citation//gmd:date" />
              <dl class="dl-horizontal">
                <dt>Propriétaire(s)</dt>
                <dd>
                  <xsl:value-of select="string-join($mdOrgName, ', ')"/>
                  <!-- contact email -->
                  <xsl:variable name="eMails" select='string-join($contactMailAddress, ",")' />
                  <xsl:if test="$eMails != ''">
                    <a href="mailto:{$eMails}"><span class="glyphicon glyphicon-envelope" /></a>
                  </xsl:if>
                </dd>
              </dl>
              <dl class="dl-horizontal">
                <dt>Limitation d'utilisation</dt>
                <dd>
                  <xsl:value-of select="string-join($mdLimitations, ', ')"/>
                </dd>
              </dl>
              <dl class="dl-horizontal">
                <dt>Mots-clés</dt>
                <dd>
                  <xsl:value-of select="string-join($mdKeywords, ', ')"/>
                </dd>
              </dl>
              <dl class="dl-horizontal">
                <dt>Aperçu</dt>
                <dd>
                  <img class="graphic-overview" src="{$graphicOverview}" alt="Aperçu"/>
                </dd>
              </dl>
            </div>
            <!-- right part: links -->
            <div class="col-md-3 text-center metadata-links">
              <!-- logo organisme -->
              <div class="row">
                <xsl:if test="$orgLogo != '' and $orgWebsite != ''">
                  <a href="{$orgWebsite}"><img class="org-logo" src="{$orgLogo}" alt="Logo"/></a>
                </xsl:if>
                <xsl:if test="$orgLogo != '' and $orgWebsite = ''">
                  <img class="org-logo" src="{$orgLogo}" alt="Logo"/>
                </xsl:if>
                <xsl:if test="$orgLogo = ''">
                  <img class="org-logo" src="/geonetwork/images/logos/PIGMA.gif" alt="Default logo"/>
                </xsl:if>
              </div>
              <!-- Lien  -->
              <div class="row vert-space">
                <a href="{$mdUrl}" class="btn btn-lg btn-primary" role="button">
                  <img src="/img/download.png" alt="téléchargement" class="icon" />
                  <p>
                    Plus d'informations
                    <br/>
                    et accès aux données
                  </p>
                </a>
              </div>
              <!-- Lien catalogue -->
              <div class="row vert-space">
                <a href="{$catalogUrl}" class="btn btn-lg btn-default" role="button" id="catalogueBtn">
                  <p>Catalogue de données</p>
                </a>
              </div>
              <!-- Accès site thématiques -->
              <div class="row vert-space btn btn-lg btn-default pigma-themes">
                <a href="/web/10157/30">
                Accès aux sites thématiques
                </a>
              </div>
            </div>
          </div>
        </div>
        <!-- logos partenaires -->
        <div class="container">
          <footer class="footer">
            <img src="/img/bandeau-partenaires.jpg" alt="logos des partenaires"/>
          </footer>
        </div>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
