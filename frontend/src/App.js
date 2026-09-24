import React, { useState, useEffect } from 'react';
import { 
  Zap, 
  Send, 
  Sparkles, 
  RefreshCw, 
  Utensils, 
  ShoppingBag, 
  Car, 
  Smartphone,
  CheckCheck
} from 'lucide-react';
import axios from 'axios';

const BACKEND_URL = 'http://localhost:8080/api/v1';

export default function App() {
  const [dashboard, setDashboard] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [activeAlert, setActiveAlert] = useState(null);
  const [loading, setLoading] = useState(true);

  // Live WhatsApp Messages on Rahul's Phone
  const [whatsAppFeed, setWhatsAppFeed] = useState([
    {
      time: '09:00 AM',
      text: "👋 Welcome Rahul! SmartSpend is monitoring payment notifications for +919876543210.\n\nIncome: ₹40,000\nBudget: ₹30,000"
    }
  ]);

  // AI Copilot Chat state
  const [copilotMessages, setCopilotMessages] = useState([
    { sender: 'ai', text: "Hello Rahul! What would you like to check about your finances?" }
  ]);
  const [inputMsg, setInputMsg] = useState('');

  const fetchDashboardData = async () => {
    try {
      const res = await axios.get(`${BACKEND_URL}/dashboard?phoneNumber=%2B919876543210`);
      setDashboard(res.data);
      setRecentTransactions(res.data.recentTransactions || []);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();

    const eventSource = new EventSource(`${BACKEND_URL}/stream`);

    eventSource.addEventListener('TRANSACTION_SAVED', (e) => {
      const payload = JSON.parse(e.data);
      const tx = payload.transaction;
      const intel = payload.intelligence;

      setRecentTransactions((prev) => [tx, ...prev]);

      // If WhatsApp messages were dispatched, show them on Rahul's WhatsApp simulator
      if (intel && intel.whatsAppDispatches && intel.whatsAppDispatches.length > 0) {
        intel.whatsAppDispatches.forEach((msg) => {
          setWhatsAppFeed((prev) => [
            ...prev,
            { time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), text: msg }
          ]);
        });
      }

      // If high/warning alert triggered, set banner
      if (intel && intel.alertType !== 'NORMAL') {
        setActiveAlert({ title: intel.alertTitle, desc: intel.alertDescription });
      }

      fetchDashboardData();
    });

    return () => eventSource.close();
  }, []);

  const simulatePayment = async (rawText) => {
    try {
      await axios.post(`${BACKEND_URL}/transactions/ingest`, {
        phoneNumber: "+919876543210",
        rawText: rawText
      });
    } catch (err) {
      console.error(err);
    }
  };

  const handleSendCopilot = async (e) => {
    e.preventDefault();
    if (!inputMsg.trim()) return;

    const userText = inputMsg;
    setCopilotMessages((prev) => [...prev, { sender: 'user', text: userText }]);
    setInputMsg('');

    try {
      const res = await axios.post(`${BACKEND_URL}/ai/chat`, { message: userText });
      setCopilotMessages((prev) => [...prev, { sender: 'ai', text: res.data.reply }]);
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center text-emerald-400">
        <RefreshCw className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  const foodSpent = Number(dashboard?.foodSpend) || 0;
  const shoppingSpent = Number(dashboard?.shoppingSpend) || 0;
  const travelSpent = Number(dashboard?.travelSpend) || 0;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Bar */}
      <header className="border-b border-slate-800 bg-slate-900/60 px-8 py-4 flex justify-between items-center sticky top-0 z-50">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center font-bold text-emerald-400 text-lg">
            ₹
          </div>
          <div>
            <h1 className="font-bold text-base leading-tight">SmartSpend</h1>
            <p className="text-xs text-slate-400">Personal Autonomous Intelligence</p>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs">
          <span className="px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> Mobile Listener Connected
          </span>
          <span className="text-slate-400">User: <strong className="text-slate-200">Rahul (+919876543210)</strong></span>
        </div>
      </header>

      {/* Main 3-Column Layout */}
      <main className="flex-1 p-8 max-w-7xl mx-auto w-full grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* Left Column (5 Cols): Budgets & Ingestion Simulators */}
        <div className="lg:col-span-5 space-y-5">
          
          {/* Top Metrics Row */}
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-slate-900/80 border border-slate-800 p-4 rounded-xl">
              <span className="text-[10px] text-slate-400 uppercase font-semibold">Monthly Income</span>
              <p className="text-xl font-black mt-1">₹40,000</p>
            </div>
            <div className="bg-slate-900/80 border border-slate-800 p-4 rounded-xl">
              <span className="text-[10px] text-indigo-400 uppercase font-semibold">Total Budget</span>
              <p className="text-xl font-black text-indigo-400 mt-1">₹30,000</p>
            </div>
            <div className="bg-slate-900/80 border border-slate-800 p-4 rounded-xl">
              <span className="text-[10px] text-amber-400 uppercase font-semibold">Food Budget</span>
              <p className="text-xl font-black text-amber-400 mt-1">₹5,000</p>
            </div>
          </div>

          {/* Quick Simulation Buttons (Rahul's Story Sequence) */}
          <div className="bg-slate-900/40 border border-slate-800 p-4 rounded-xl">
            <h3 className="text-xs uppercase text-slate-400 font-semibold mb-3 flex items-center gap-1.5">
              <Zap className="w-3.5 h-3.5 text-emerald-400" /> Simulate Real Payment Notifications
            </h3>
            <div className="flex flex-col gap-2">
              <button 
                onClick={() => simulatePayment("₹280 debited. Payment to Swiggy successful.")}
                className="w-full text-left text-xs bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 p-2.5 rounded-lg transition flex justify-between"
              >
                <span>🟢 <strong>Day 1</strong>: Biryani Order</span>
                <span className="text-slate-400">Swiggy ₹280</span>
              </button>

              <button 
                onClick={() => {
                  simulatePayment("₹350 debited for Swiggy order.");
                  setTimeout(() => simulatePayment("₹420 spent at Zomato."), 400);
                }}
                className="w-full text-left text-xs bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 p-2.5 rounded-lg transition flex justify-between"
              >
                <span>🟢 <strong>Day 3</strong>: Food Orders</span>
                <span className="text-slate-400">₹350 + ₹420 (₹1,050 total)</span>
              </button>

              <button 
                onClick={() => {
                  simulatePayment("₹450 debited at Swiggy.");
                  setTimeout(() => simulatePayment("₹380 spent at Zomato."), 300);
                  setTimeout(() => simulatePayment("₹320 paid to Swiggy."), 600);
                }}
                className="w-full text-left text-xs bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 p-2.5 rounded-lg transition flex justify-between"
              >
                <span>🔴 <strong>Day 6</strong>: Food Surge (+69%)</span>
                <span>₹2,200 (Forecast ₹7,800)</span>
              </button>

              <button 
                onClick={() => simulatePayment("₹18000 debited for Amazon purchase.")}
                className="w-full text-left text-xs bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/30 p-2.5 rounded-lg transition flex justify-between"
              >
                <span>🚨 <strong>Day 10</strong>: Unusual Outlier</span>
                <span>Amazon ₹18,000</span>
              </button>

              <button 
                onClick={() => {
                  simulatePayment("₹649 debited for Netflix subscription.");
                  setTimeout(() => simulatePayment("₹119 spent at Spotify."), 300);
                  setTimeout(() => simulatePayment("₹299 debited for Amazon Prime."), 600);
                  setTimeout(() => simulatePayment("₹799 paid for Internet bill."), 900);
                }}
                className="w-full text-left text-xs bg-purple-500/10 hover:bg-purple-500/20 text-purple-300 border border-purple-500/30 p-2.5 rounded-lg transition flex justify-between"
              >
                <span>🔄 <strong>Subscriptions</strong>: Recurring</span>
                <span>4 Services (₹1,866/mo)</span>
              </button>
            </div>
          </div>

          {/* Active Alert Banner */}
          {activeAlert && (
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs">
              <strong className="block text-sm mb-1">{activeAlert.title}</strong>
              {activeAlert.desc}
            </div>
          )}

          {/* Budget Consumption Bars */}
          <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl space-y-3">
            <h4 className="text-xs font-semibold text-slate-300">Category Budgets</h4>
            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="flex items-center gap-1.5"><Utensils className="w-3 h-3 text-amber-400" /> Food</span>
                <span>₹{foodSpent} / ₹5,000</span>
              </div>
              <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                <div 
                  className={`h-full ${foodSpent > 5000 ? 'bg-rose-500' : 'bg-amber-400'}`} 
                  style={{ width: `${Math.min(100, (foodSpent / 5000) * 100)}%` }}
                ></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="flex items-center gap-1.5"><ShoppingBag className="w-3 h-3 text-rose-400" /> Shopping</span>
                <span>₹{shoppingSpent} / ₹4,000</span>
              </div>
              <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                <div 
                  className={`h-full ${shoppingSpent > 4000 ? 'bg-rose-500' : 'bg-rose-400'}`} 
                  style={{ width: `${Math.min(100, (shoppingSpent / 4000) * 100)}%` }}
                ></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between text-xs mb-1">
                <span className="flex items-center gap-1.5"><Car className="w-3 h-3 text-sky-400" /> Travel</span>
                <span>₹{travelSpent} / ₹3,000</span>
              </div>
              <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                <div className="h-full bg-sky-400" style={{ width: `${Math.min(100, (travelSpent / 3000) * 100)}%` }}></div>
              </div>
            </div>
          </div>

        </div>

        {/* Middle Column (4 Cols): Rahul's WhatsApp Phone Screen */}
        <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-2xl flex flex-col h-[740px] overflow-hidden shadow-2xl">
          {/* WhatsApp Header */}
          <div className="bg-[#1f2c34] p-3 flex items-center justify-between border-b border-slate-700/50">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center font-bold text-white text-xs">
                SS
              </div>
              <div>
                <h3 className="text-xs font-bold text-slate-100 leading-tight">SmartSpend WhatsApp</h3>
                <span className="text-[10px] text-emerald-400 flex items-center gap-1">Online • Official Business</span>
              </div>
            </div>
            <Smartphone className="w-4 h-4 text-slate-400" />
          </div>

          {/* WhatsApp Messages Feed */}
          <div className="flex-1 p-3 overflow-y-auto space-y-2.5 bg-[#0b141a]">
            {whatsAppFeed.map((msg, idx) => (
              <div key={idx} className="bg-[#1f2c34] text-slate-100 p-3 rounded-xl rounded-tl-none max-w-[90%] text-xs shadow">
                <p className="whitespace-pre-line leading-relaxed">{msg.text}</p>
                <div className="flex justify-end items-center gap-1 mt-1 text-[9px] text-slate-400">
                  <span>{msg.time}</span>
                  <CheckCheck className="w-3 h-3 text-sky-400" />
                </div>
              </div>
            ))}
          </div>

          <div className="p-2.5 bg-[#1f2c34] text-[10px] text-slate-400 text-center border-t border-slate-800">
            🔒 Dispatched via Meta Cloud API to +919876543210
          </div>
        </div>

        {/* Right Column (3 Cols): AI Copilot Chat */}
        <div className="lg:col-span-3 bg-slate-900/80 border border-slate-800 rounded-2xl flex flex-col h-[740px] overflow-hidden">
          <div className="p-3 border-b border-slate-800 flex items-center gap-2 bg-slate-900">
            <Sparkles className="w-4 h-4 text-emerald-400" />
            <h2 className="font-semibold text-xs">AI Financial Copilot</h2>
          </div>

          <div className="flex-1 p-3 overflow-y-auto space-y-2 text-xs">
            {copilotMessages.map((m, idx) => (
              <div 
                key={idx} 
                className={`p-3 rounded-xl max-w-[90%] whitespace-pre-line leading-relaxed ${
                  m.sender === 'user' 
                    ? 'bg-indigo-600 text-white ml-auto rounded-tr-none' 
                    : 'bg-slate-800 text-slate-200 border border-slate-700/50 rounded-tl-none'
                }`}
              >
                {m.text}
              </div>
            ))}
          </div>

          <div className="p-2 border-t border-slate-800 bg-slate-900/60 space-y-1">
            <button 
              onClick={() => setInputMsg("How much did I spend this month?")}
              className="text-[10px] text-left text-slate-400 hover:text-slate-200 block truncate"
            >
              👉 "How much did I spend this month?"
            </button>
            <button 
              onClick={() => setInputMsg("Why did I spend more this month?")}
              className="text-[10px] text-left text-slate-400 hover:text-slate-200 block truncate"
            >
              👉 "Why did I spend more this month?"
            </button>
            <button 
              onClick={() => setInputMsg("I want to save ₹50,000 in 5 months")}
              className="text-[10px] text-left text-slate-400 hover:text-slate-200 block truncate"
            >
              👉 "I want to save ₹50,000 in 5 months"
            </button>
          </div>

          <form onSubmit={handleSendCopilot} className="p-3 border-t border-slate-800 bg-slate-900 flex gap-2">
            <input 
              type="text" 
              value={inputMsg}
              onChange={(e) => setInputMsg(e.target.value)}
              placeholder="Ask Copilot..." 
              className="flex-1 bg-slate-950 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-500"
            />
            <button type="submit" className="bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1.5 rounded-lg text-xs font-semibold">
              <Send className="w-3 h-3" />
            </button>
          </form>
        </div>

      </main>
    </div>
  );
}