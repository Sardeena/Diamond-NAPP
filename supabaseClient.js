/**
 * Supabase Client Utility File (supabaseClient.js)
 * 
 * Provides a highly secure, reliable, and portable interface to Supabase.
 * It detects the execution context and dynamically integrates the official 
 * `@supabase/supabase-js` client library if available. If not, it falls back to a 
 * lightweight, high-performance HTTP REST Client engine that mimics the Supabase 
 * API syntax exactly—handling Auth sessions, JWT state, and database CRUD.
 */

const fs = require('fs');
const path = require('path');

// 1. DYNAMIC ENVIRONMENT VARIABLE RESOLVER
let supabaseUrl = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL || '';
let supabaseKey = process.env.SUPABASE_KEY || process.env.NEXT_PUBLIC_SUPABASE_KEY || '';

// If env is empty and we are running in a Node environment, try to read .env file manually
if (typeof process !== 'undefined' && (!supabaseUrl || !supabaseKey)) {
  try {
    const envPath = path.resolve(process.cwd(), '.env');
    if (fs.existsSync(envPath)) {
      const envContent = fs.readFileSync(envPath, 'utf-8');
      envContent.split('\n').forEach(line => {
        const match = line.match(/^\s*([\w.-]+)\s*=\s*(.*)?\s*$/);
        if (match) {
          const key = match[1];
          let value = match[2] || '';
          if (value.length > 0 && value.startsWith('"') && value.endsWith('"')) {
            value = value.substring(1, value.length - 1);
          }
          if (key === 'SUPABASE_URL' && !supabaseUrl) supabaseUrl = value.trim();
          if (key === 'SUPABASE_KEY' && !supabaseKey) supabaseKey = value.trim();
        }
      });
    }
  } catch (e) {
    // Silent fail in environments where fs or path are unavailable
  }
}

// Ensure clean URL structure
if (supabaseUrl) {
  supabaseUrl = supabaseUrl.trim().replace(/\/$/, '');
}
if (supabaseKey) {
  supabaseKey = supabaseKey.trim();
}

// 2. SESSION STORAGE CONTROLLER (Safe for Web, React Native, and Node Server contexts)
const sessionStore = {
  get: () => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const stored = window.localStorage.getItem('sb-session');
        return stored ? JSON.parse(stored) : null;
      }
    } catch (e) {
      console.warn('[SupabaseClient] Error reading session from localStorage:', e);
    }
    return global._sb_session || null;
  },
  set: (session) => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        if (session) {
          window.localStorage.setItem('sb-session', JSON.stringify(session));
        } else {
          window.localStorage.removeItem('sb-session');
        }
      }
    } catch (e) {
      console.warn('[SupabaseClient] Error saving session to localStorage:', e);
    }
    if (session) {
      global._sb_session = session;
    } else {
      delete global._sb_session;
    }
  }
};

let supabaseInstance = null;

// Helper to determine if we should fall back to custom implementation
let useFallback = true;

try {
  // Try to load the official Supabase library if present
  const { createClient } = require('@supabase/supabase-js');
  if (createClient && supabaseUrl && supabaseKey) {
    supabaseInstance = createClient(supabaseUrl, supabaseKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
      }
    });
    useFallback = false;
    console.log('[SupabaseClient] Successfully initialized via official @supabase/supabase-js library.');
  }
} catch (err) {
  // Library not present, we will use our lightweight HTTP fallback client
}

if (useFallback) {
  console.log('[SupabaseClient] Initializing lightweight custom HTTP engine fallback.');

  // Custom client instance that matches official Supabase syntax
  class CustomSupabaseClient {
    constructor(url, key) {
      this.url = url;
      this.key = key;

      // AUTH SUB-SYSTEM
      this.auth = {
        signUp: async ({ email, password }) => {
          return this._request('/auth/v1/signup', 'POST', { email, password });
        },
        signInWithPassword: async ({ email, password }) => {
          const result = await this._request('/auth/v1/token?grant_type=password', 'POST', { email, password });
          if (result.data && result.data.access_token) {
            sessionStore.set(result.data);
          }
          return result;
        },
        signOut: async () => {
          const session = sessionStore.get();
          if (session && session.access_token) {
            await this._request('/auth/v1/logout', 'POST', {}, session.access_token);
          }
          sessionStore.set(null);
          return { error: null };
        },
        getSession: async () => {
          const session = sessionStore.get();
          return { data: { session }, error: null };
        },
        getUser: async () => {
          const session = sessionStore.get();
          if (!session || !session.access_token) {
            return { data: { user: null }, error: new Error('No active session.') };
          }
          const res = await this._request('/auth/v1/user', 'GET', null, session.access_token);
          return { data: { user: res.data }, error: res.error };
        }
      };
    }

    // QUERY BUILDER FOR DATABASE
    from(tableName) {
      const client = this;
      let queryParams = [];
      let filters = [];
      let method = 'GET';
      let bodyData = null;

      const builder = {
        select: (columns = '*') => {
          method = 'GET';
          if (columns !== '*') {
            queryParams.push(`select=${encodeURIComponent(columns)}`);
          }
          return builder;
        },
        insert: (data) => {
          method = 'POST';
          bodyData = data;
          return builder;
        },
        update: (data) => {
          method = 'PATCH';
          bodyData = data;
          return builder;
        },
        delete: () => {
          method = 'DELETE';
          return builder;
        },
        eq: (column, value) => {
          filters.push(`${column}=eq.${encodeURIComponent(value)}`);
          return builder;
        },
        execute: async () => {
          let requestPath = `/rest/v1/${tableName}`;
          const allParams = [...queryParams, ...filters];
          if (allParams.length > 0) {
            requestPath += `?${allParams.join('&')}`;
          }
          const session = sessionStore.get();
          const token = session ? session.access_token : null;
          return client._request(requestPath, method, bodyData, token);
        },
        // Support direct thenable for promise-like chaining
        then: function (onfulfilled, onrejected) {
          return builder.execute().then(onfulfilled, onrejected);
        }
      };

      return builder;
    }

    // PRIVATE NETWORK REQUEST DISPATCHER
    async _request(endpoint, method, body = null, token = null) {
      if (!this.url || !this.key) {
        return { data: null, error: new Error('Supabase URL or Key is not configured.') };
      }

      const headers = {
        'apikey': this.key,
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      } else {
        headers['Authorization'] = `Bearer ${this.key}`;
      }

      if (method === 'POST' || method === 'PATCH') {
        headers['Prefer'] = 'return=representation';
      }

      const options = {
        method,
        headers,
      };

      if (body && (method === 'POST' || method === 'PATCH')) {
        options.body = JSON.stringify(body);
      }

      try {
        const response = await fetch(`${this.url}${endpoint}`, options);
        const text = await response.text();
        let data = null;
        try {
          data = text ? JSON.parse(text) : null;
        } catch (e) {
          data = text;
        }

        if (!response.ok) {
          return { data: null, error: data || { message: `Request failed with status ${response.status}` } };
        }

        return { data, error: null };
      } catch (err) {
        return { data: null, error: err };
      }
    }
  }

  supabaseInstance = new CustomSupabaseClient(supabaseUrl, supabaseKey);
}

// 3. EXPORT THE INSTANCE AND HELPERS
module.exports = {
  supabase: supabaseInstance,
  supabaseUrl,
  supabaseKey,
  isConfigured: () => {
    return !!(supabaseUrl && supabaseKey && !supabaseUrl.includes('placeholder'));
  },
  sessionStore
};
