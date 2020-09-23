/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

(function() {
  goog.provide('gn_esri_service');

  var module = angular.module('gn_esri_service', []);

  var PADDING = 5;
  var TITLE_PADDING = 15;
  var FONT_SIZE = 12;
  var TMP_IMAGE = new Image();

  module.service('gnEsriUtils', ['$q',
    function($q) {
      return {
        /**
         * Renders a JSON legend asynchronously to an image
         * @param {Object} json
         * @param {string} [layerId] optional, legend will be filtered on this layer
         * @return {Promise<string>} data url
         */
        renderLegend(json, layerId) {
          var $this = this;

          var legend = !!layerId ? {
            layers: json.layers.filter(function (layer) {
              return layer.layerId == layerId;
            })
          } : json;

          var canvas = document.createElement('canvas');
          var context = canvas.getContext('2d');
          context.textBaseline = 'middle';
          context.font = FONT_SIZE + 'px sans-serif';
          var size = this.measureLegend(context, legend);

          // size canvas & draw background
          canvas.width = size[0];
          canvas.height = size[1];
          context.fillStyle = 'white';
          context.fillRect(0, 0, size[0], size[1]);

          // starting Y is 0
          var promise = $q.resolve(0);

          // chain one promise per legend
          for (var i = 0; i < legend.layers.length; i++) {
            var layer = json.layers[i];
            promise = promise.then(function (y) {
              var layer = this;
              y += TITLE_PADDING;
              context.fillStyle = 'black';
              context.textBaseline = 'middle';
              context.font = 'bold ' + FONT_SIZE + 'px sans-serif';
              context.fillText(layer.layerName, PADDING, y + FONT_SIZE / 2);
              y += FONT_SIZE;
              return $this.renderRules(y, context, layer.legend);
            }.bind(layer));
          }

          return promise.then(function() {
            return canvas.toDataURL('image/png');
          });
        },

        /**
         * Renders a array of rules asynchronously
         * @param {number} currentY
         * @param {CanvasRenderingContext2D} context
         * @param {Object[]} rules
         * @return {Promise<number>} current y
         */
        renderRules(currentY, context, rules) {
          var $this = this;
          var promise = $q.resolve(currentY);

          // chain one promise for each rule
          for (var i = 0; i < rules.length; i++) {
            var rule = rules[i];
            promise = promise.then(function (y) {
              var rule = this;
              return $this.renderImageData(rule.imageData, rule.contentType).then(function (image) {
                y += PADDING;
                context.drawImage(image, PADDING, y, rule.width, rule.height);
                context.fillStyle = 'black';
                context.textBaseline = 'middle';
                context.font = FONT_SIZE + 'px sans-serif';
                context.fillText(rule.label, PADDING * 2 + rule.width, y + rule.height / 2);
                return y + rule.height;
              })
            }.bind(rule));
          }

          return promise;
        },

        /**
         * Returns a promise resolving on an Image element
         * with the data loaded
         * @param {string} imageData base-64 encoded image data
         * @param {string} format, defaults to 'image/png'
         * @return {Promise<Image>} image
         */
        renderImageData(imageData, format) {
          var defer = $q.defer();
          TMP_IMAGE.onload = function() {
            defer.resolve(this);
          };
          TMP_IMAGE.src = 'data:' + (format || 'image/png') + ';base64,' + imageData;
          return defer.promise;
        },

        /**
         * Returns the expected size of the legend
         * @param {CanvasRenderingContext2D} context
         * @param {Object} json
         * @return {[number, number]} width and height
         */
        measureLegend(context, json) {
          var width = 100;
          var height = 0;
          for (var i = 0; i < json.layers.length; i++) {
            var layer = json.layers[i];
            var nameMetrics = context.measureText(layer.layerName);
            width = Math.max(width, nameMetrics.width + PADDING * 2);
            height += TITLE_PADDING + FONT_SIZE;

            for (var j = 0; j < layer.legend.length; j++) {
              var rule = layer.legend[j];
              var ruleMetrics = context.measureText(rule.label);
              width = Math.max(width, rule.width + ruleMetrics.width + PADDING * 3);
              height += PADDING + rule.height;
            }
          }
          return [width, height];
        }
      }
    }
  ]);
})();
