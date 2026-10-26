import{c as n,l as t}from"./index-DpBrqzcU.js";/**
 * @license lucide-react v0.441.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const i=n("Info",[["circle",{cx:"12",cy:"12",r:"10",key:"1mglay"}],["path",{d:"M12 16v-4",key:"1dtifu"}],["path",{d:"M12 8h.01",key:"e9boi3"}]]);/**
 * @license lucide-react v0.441.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const r=n("Trash2",[["path",{d:"M3 6h18",key:"d0wm0j"}],["path",{d:"M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",key:"4alrt4"}],["path",{d:"M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",key:"v07s0e"}],["line",{x1:"10",x2:"10",y1:"11",y2:"17",key:"1uufr5"}],["line",{x1:"14",x2:"14",y1:"11",y2:"17",key:"xtxkd"}]]),o={getShipments:async a=>(await t.get("/api/v1/shipments",{params:a})).data.data,getShipment:async a=>(await t.get(`/api/v1/shipments/${a}`)).data.data,getShipmentByTracking:async a=>(await t.get(`/api/v1/shipments/tracking/${encodeURIComponent(a)}`)).data.data,createShipment:async a=>(await t.post("/api/v1/shipments",a)).data.data,updateShipment:async(a,e)=>(await t.put(`/api/v1/shipments/${a}`,e)).data.data,assignTrip:async(a,e)=>(await t.patch(`/api/v1/shipments/${a}/assign-trip`,e)).data.data,updateStatus:async(a,e)=>(await t.patch(`/api/v1/shipments/${a}/status`,e)).data.data,completeDelivery:async(a,e)=>(await t.post(`/api/v1/shipments/${a}/deliver`,e??{})).data.data,saveWaypoints:async(a,e)=>(await t.put(`/api/v1/shipments/${a}/waypoints`,e)).data.data,getWaypoints:async(a,e)=>(await t.get(`/api/v1/shipments/${a}/waypoints`,{params:e})).data.data,getTimeline:async a=>(await t.get(`/api/v1/shipments/${a}/timeline`)).data.data,checkDelayedShipments:async()=>(await t.post("/api/v1/shipments/check-delayed")).data.data,getAvailableTrips:async()=>{var e;return((e=(await t.get("/api/v1/trips",{params:{size:50}})).data.data)==null?void 0:e.content)??[]}};export{i as I,r as T,o as s};
